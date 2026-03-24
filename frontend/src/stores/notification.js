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
    pollingTimer: null // [시니어 조치] 폴링 타이머 추가
  }),

  actions: {
    // [시니어 조치] 통합 상태 동기화 (숫자와 목록을 한꺼번에 최신화)
    async syncState() {
      if (this.isMarkingAll) return
      const authStore = useAuthStore()
      if (!authStore.isAuthenticated) return

      try {
        console.log('[알림-DEBUG] 서버 상태 동기화 중...')
        // 1. 카운트와 목록을 병렬로 요청하여 속도 최적화
        const [listRes, countRes] = await Promise.all([
          axios.get(`/api/notifications?_t=${Date.now()}`),
          this.fetchUnreadCount()
        ])

        // 2. 알림 목록 정규화 및 저장
        const newItems = (listRes.data.data || []).map(n => ({
          ...n,
          isRead: n.isRead === true || n.read === true || n.isRead === 1 || n.read === 1 || n.isRead === 'true' || n.read === 'true'
        }))
        
        this.notifications = newItems
        this.isLoaded = true
        console.log('[알림-DEBUG] 동기화 완료:', this.notifications.length, '건, 미읽음:', this.unreadCount)
      } catch (e) {
        console.error('[알림-DEBUG] 동기화 실패:', e)
      }
    },

    async fetchUnreadCount() {
      try {
        const res = await axios.get(`/api/notifications/unread-count?_t=${Date.now()}`)
        this.unreadCount = Number(res.data.data) || 0
        return res
      } catch (e) {
        throw e
      }
    },

    // [시니어 조치] 초기화 로직 (동기화 + SSE + 폴링 시작)
    async initialize(callback) {
      const authStore = useAuthStore()
      
      if (this.isInitializing) return
      if (this.isLoaded && this.eventSource && this.eventSource.readyState === 1) return

      this.isInitializing = true 

      let retryCount = 0
      const checkAuthAndRun = async () => {
        if (authStore.isAuthenticated && authStore.accessToken) {
          try {
            await this.syncState() // 1. 초기 데이터 로드
            this.connectSse(callback) // 2. 실시간 채널 연결
            this.startPolling(callback) // 3. 안전장치 폴링 시작
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

    // [시니어 조치] 30초 주기 안전장치 폴링
    startPolling(callback) {
      this.stopPolling()
      this.pollingTimer = setInterval(() => {
        console.log('[알림-DEBUG] 안전장치 폴링 실행 중...')
        const prevCount = this.unreadCount
        this.syncState().then(() => {
          // 폴링 중 새로운 알림이 발견되면 콜백 호출 (토스트 띄우기용)
          if (this.unreadCount > prevCount && callback && this.notifications.length > 0) {
            callback(this.notifications[0])
          }
        })
      }, 30000) // 30초 주기 (서버 부하 최소화 및 정합성 보장)
    },

    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }
    },

    async connectSse(callback) {
      const authStore = useAuthStore()
      if (!authStore.isAuthenticated) return
      if (this.isConnecting) return
      if (this.eventSource && (this.eventSource.readyState === 0 || this.eventSource.readyState === 1)) return

      this.isConnecting = true

      try {
        // 연결 전 토큰 상태 한 번 더 체크
        await axios.get('/api/notifications/unread-count?_t=' + Date.now())
        authStore.syncTokenFromStorage()
        
        this.disconnectSse()

        const token = authStore.accessToken
        const url = `/api/notifications/subscribe?token=${encodeURIComponent(token)}&_t=${Date.now()}`
        
        this.eventSource = new EventSource(url)

        this.eventSource.onopen = () => {
          console.log('[SSE-DEBUG] 연결 성공')
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

            // 중복 방지 및 최상단 추가
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
          this.disconnectSse()
          if (authStore.isAuthenticated) {
            if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
            this.reconnectTimer = setTimeout(() => this.connectSse(callback), 15000)
          }
        }
      } catch (e) {
        console.error('[SSE-DEBUG] 연결 실패:', e)
      } finally {
        this.isConnecting = false 
      }
    },

    async markAsRead(id) {
      const index = this.notifications.findIndex(n => n.id === id)
      if (index !== -1 && !this.notifications[index].isRead) {
        this.notifications[index].isRead = true
        this.unreadCount = Math.max(0, this.unreadCount - 1)
        try {
          await axios.patch(`/api/notifications/${id}/read`)
        } catch (e) {
          console.error('읽음 처리 실패', e)
        }
      }
    },

    async markAllAsRead() {
      if (this.unreadCount === 0) return
      this.isMarkingAll = true
      this.notifications = this.notifications.map(n => ({ ...n, isRead: true }))
      this.unreadCount = 0
      try {
        await axios.patch('/api/notifications/read-all')
        setTimeout(() => { this.isMarkingAll = false }, 1000)
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
