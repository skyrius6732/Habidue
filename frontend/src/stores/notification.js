import { defineStore } from 'pinia'
import axios from '@/plugins/axios'
import { useAuthStore } from './auth'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    unreadCount: 0,
    eventSource: null,
    isMarkingAll: false,
    isLoaded: false,
    isInitializing: false,
    isConnecting: false,
    reconnectTimer: null,
    pollingTimer: null,
    currentPage: 0,
    hasMore: true,
    isLoadingMore: false
  }),

  actions: {
    // [시니어 조치] 초기 로딩 및 다음 페이지 로딩 통합
    async fetchNotifications(isInitial = true) {
      if (isInitial) {
        this.currentPage = 0
        this.hasMore = true
      }
      if (!this.hasMore || this.isLoadingMore) return
      
      this.isLoadingMore = true
      try {
        const res = await axios.get(`/api/notifications?page=${this.currentPage}&size=20&_t=${Date.now()}`)
        const fetchedNotis = (res.data.data || []).map(n => ({
          ...n,
          isRead: n.isRead === true || n.read === true || n.isRead === 1 || n.read === 1 || n.isRead === 'true' || n.read === 'true'
        }))

        if (isInitial) {
          this.notifications = fetchedNotis
        } else {
          // 중복 제거하며 추가
          const existingIds = new Set(this.notifications.map(n => n.id))
          const newUniqueNotis = fetchedNotis.filter(n => !existingIds.has(n.id))
          this.notifications = [...this.notifications, ...newUniqueNotis]
        }

        this.hasMore = fetchedNotis.length === 20
        this.currentPage++
        this.isLoaded = true
      } catch (e) {
        console.warn('알림 페칭 실패', e)
      } finally {
        this.isLoadingMore = false
      }
    },

    // [시니어 조치] 통합 상태 동기화 (기존 코드를 fetchNotifications 활용으로 대체 가능하나 하위 호환 위해 유지)
    async syncState() {
      if (this.isMarkingAll) return
      const authStore = useAuthStore()
      if (!authStore.isAuthenticated) return

      try {
        // 동기화는 항상 최신 첫 페이지만 갱신
        const [listRes, countRes] = await Promise.all([
          axios.get(`/api/notifications?page=0&size=20&_t=${Date.now()}`),
          axios.get(`/api/notifications/unread-count?_t=${Date.now()}`)
        ])

        const newItems = (listRes.data.data || []).map(n => ({
          ...n,
          isRead: n.isRead === true || n.read === true || n.isRead === 1 || n.read === 1 || n.isRead === 'true' || n.read === 'true'
        }))
        
        // 실시간성 유지를 위해 기존 목록의 첫 페이지 부분만 교체하거나 병합
        this.notifications = newItems
        this.unreadCount = Number(countRes.data.data) || 0
        this.isLoaded = true
        this.currentPage = 1 // 첫 페이지 로드 완료 상태
        this.hasMore = newItems.length === 20
      } catch (e) {
        // ERR_CONNECTION_RESET 등 네트워크 에러는 SSE 끊김 시 정상적으로 발생하므로 조용히 무시
        if (e?.code !== 'ERR_NETWORK' && e?.message !== 'Network Error') {
          console.warn('[알림-DEBUG] 동기화 중 일시적 오류 발생 (자동 재시도 예정)')
        }
      }
    },

    async initialize(callback) {
      const authStore = useAuthStore()
      if (this.isInitializing) return
      if (this.isLoaded && this.eventSource && this.eventSource.readyState === 1) return

      this.isInitializing = true 
      let retryCount = 0
      const checkAuthAndRun = async () => {
        if (authStore.isAuthenticated && authStore.accessToken) {
          try {
            await this.syncState()
            await this.connectSse(callback)
            this.startPolling(callback)
          } finally {
            this.isInitializing = false 
          }
          return
        }
        if (retryCount < 20) {
          retryCount++
          setTimeout(checkAuthAndRun, 500)
        } else {
          this.isInitializing = false 
        }
      }
      checkAuthAndRun()
    },

    startPolling(callback) {
      this.stopPolling()
      // SSE가 정상 연결된 상태면 폴링 불필요 (30초마다 체크하여 SSE 상태에 따라 자동 조절)
      this.pollingTimer = setInterval(() => {
        if (this.eventSource && this.eventSource.readyState === 1) return // SSE OPEN이면 스킵
        const prevCount = this.unreadCount
        this.syncState().then(() => {
          if (this.unreadCount > prevCount && callback && this.notifications.length > 0) {
            callback(this.notifications[0])
          }
        })
      }, 30000)
    },

    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }
    },

    /**
     * [시니어 조치] SSE 연결 전 토큰 체크 로직의 안정성 극대화
     */
    async connectSse(callback) {
      const authStore = useAuthStore()
      if (!authStore.isAuthenticated) return
      if (this.isConnecting) return
      
      // 이미 연결이 시도 중이거나 열려 있으면 중복 실행 원천 차단
      if (this.eventSource && (this.eventSource.readyState === 0 || this.eventSource.readyState === 1)) return

      this.isConnecting = true

      try {
        // [중요] 연결 전 토큰 체크
        try {
          await axios.get('/api/notifications/unread-count?_t=' + Date.now())
          // axios 인터셉터에 의해 토큰이 갱신되었을 수 있으므로 다시 가져옴
          authStore.syncTokenFromStorage()
        } catch (tokenErr) {
          console.warn('[SSE-DEBUG] 토큰 체크 실패, 2초 후 재시도 (갱신 대기)')
          this.isConnecting = false
          setTimeout(() => this.connectSse(callback), 2000)
          return
        }
        
        this.disconnectSse()

        const token = authStore.accessToken
        if (!token) {
          this.isConnecting = false
          return
        }
        const url = `/api/notifications/subscribe?token=${encodeURIComponent(token)}&_t=${Date.now()}`
        
        this.eventSource = new EventSource(url)

        this.eventSource.onopen = () => {
          console.log('[SSE-DEBUG] 실시간 채널 연결 성공')
        }

        this.eventSource.onmessage = (event) => {
          if (event.data === 'connected' || event.data === 'ping') return
          try {
            const rawData = JSON.parse(event.data)
            if (!rawData || !rawData.id) return
            const newNoti = {
              ...rawData,
              isRead: rawData.isRead === true || rawData.read === true || rawData.isRead === 1 || rawData.read === 1 || rawData.isRead === 'true' || rawData.read === 'true'
            }
            if (!this.notifications.some(n => n.id === newNoti.id)) {
              this.notifications.unshift(newNoti)
              this.unreadCount++
              if (callback) callback(newNoti)
            }
          } catch (e) {
            console.error('[SSE-DEBUG] 파싱 오류:', e)
          }
        }

        this.eventSource.onerror = (e) => {
          console.warn('[SSE-DEBUG] 연결 끊김, 15초 후 재연결 시도...')
          this.disconnectSse()
          if (authStore.isAuthenticated) {
            // SSE 끊어진 동안 폴링으로 즉시 한 번 동기화 (연결 리셋 직후 대기 후 실행)
            setTimeout(() => this.syncState(), 2000)
            if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
            this.reconnectTimer = setTimeout(() => this.connectSse(callback), 15000)
          }
        }
      } catch (e) {
        console.error('[SSE-DEBUG] SSE 초기화 중 치명적 오류:', e)
      } finally {
        this.isConnecting = false 
      }
    },

    async markAsRead(id) {
      const index = this.notifications.findIndex(n => n.id === id)
      if (index !== -1 && !this.notifications[index].isRead) {
        this.notifications[index].isRead = true
        this.unreadCount = Math.max(0, this.unreadCount - 1)

        // SSE 끊김 직후 커넥션 리셋에 걸릴 수 있으므로 최대 3회 재시도
        const isNetworkError = (e) => e?.code === 'ERR_NETWORK' || e?.message === 'Network Error' || e?.code === 'ERR_CONNECTION_RESET'
        for (let attempt = 1; attempt <= 3; attempt++) {
          try {
            await axios.patch(`/api/notifications/${id}/read`)
            return
          } catch (e) {
            if (attempt < 3 && isNetworkError(e)) {
              await new Promise(r => setTimeout(r, attempt * 1500))
            } else if (!isNetworkError(e)) {
              console.warn('읽음 처리 실패 (서버 오류)', e?.response?.status)
              return
            }
          }
        }
        console.warn('읽음 처리 실패 (재시도 소진) - 다음 동기화 때 자동 반영됩니다')
      }
    },

    async markAllAsRead() {
      if (this.unreadCount === 0) return
      this.isMarkingAll = true
      this.notifications = this.notifications.map(n => ({ ...n, isRead: true }))
      this.unreadCount = 0
      try {
        await axios.patch('/api/notifications/read-all')
        // 락 해제 지연을 주어 연쇄적인 동기화 부하 방지
        setTimeout(() => { this.isMarkingAll = false }, 2000)
      } catch (e) {
        this.isMarkingAll = false
        await this.syncState()
      }
    },

    disconnectSse() {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }
    }
  }
})
