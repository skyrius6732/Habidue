import { defineStore } from 'pinia'
import axios from '@/plugins/axios'
import { useAuthStore } from './auth'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    unreadCount: 0,
    eventSource: null,
    isMarkingAll: false,
    isLoaded: false
  }),

  actions: {
    // [시니어 조치] 과거 내역과 실시간 연결을 원자적으로 초기화 (최후의 마스터 로직)
    async initialize(callback) {
      const authStore = useAuthStore()
      
      // 1. 이미 데이터가 있거나 연결 중이면 중복 실행 방지
      if (this.isLoaded && this.eventSource) return

      // 2. 인증 정보가 아직 없다면, 생길 때까지 짧은 주기로 체크 (최대 10초간)
      let retryCount = 0
      const checkAuthAndRun = async () => {
        if (authStore.isAuthenticated && authStore.accessToken) {
          console.log('[SSE-DEBUG] 인증 확인됨. 알림 시스템 초기화 시작...')
          await this.fetchNotifications() // 과거 내역 로드
          this.connectSse(callback) // 실시간 연결
          return
        }

        if (retryCount < 20) {
          retryCount++
          setTimeout(checkAuthAndRun, 500)
        } else {
          console.warn('[SSE-DEBUG] 인증 정보 복구 타임아웃.')
        }
      }

      checkAuthAndRun()
    },

    // [시니어 조치] 서버 데이터 정규화 및 캐시 무효화
    async fetchNotifications() {
      if (this.isMarkingAll) return 
      try {
        const res = await axios.get(`/api/notifications?_t=${Date.now()}`)
        this.notifications = res.data.data.map(n => ({
          ...n,
          isRead: n.isRead === true || n.read === true || n.isRead === 1 || n.read === 1 || n.isRead === 'true' || n.read === 'true'
        }))
        this.syncUnreadCount()
        this.isLoaded = true 
      } catch (e) {
        console.error('알림 로드 실패', e)
      }
    },

    async fetchUnreadCount() {
      if (this.isMarkingAll) return 
      try {
        const res = await axios.get(`/api/notifications/unread-count?_t=${Date.now()}`)
        this.unreadCount = Number(res.data.data) || 0
      } catch (e) {}
    },

    syncUnreadCount() {
      this.unreadCount = this.notifications.filter(n => !n.isRead).length
    },

    async markAsRead(id) {
      const index = this.notifications.findIndex(n => n.id === id)
      if (index !== -1 && !this.notifications[index].isRead) {
        this.notifications[index].isRead = true
        this.syncUnreadCount()
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
        await this.fetchNotifications()
      }
    },

    connectSse(callback) {
      const authStore = useAuthStore()
      if (!authStore.isAuthenticated || !authStore.accessToken) return

      if (this.eventSource && (this.eventSource.readyState === 0 || this.eventSource.readyState === 1)) {
        return
      }

      this.disconnectSse()

      const token = authStore.accessToken
      const url = `/api/notifications/subscribe?token=${encodeURIComponent(token)}&_t=${Date.now()}`
      
      this.eventSource = new EventSource(url)

      this.eventSource.onopen = () => {
        console.log('[SSE-DEBUG] 연결 성공: readyState =', this.eventSource.readyState)
      }

      const handleMessage = (event) => {
        console.log('[SSE-DEBUG] 메시지 수신:', event.data)
        if (event.data === 'connected!') return

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
          console.error('[SSE-DEBUG] JSON 파싱 오류:', e)
        }
      }

      this.eventSource.onmessage = handleMessage
      this.eventSource.addEventListener('message', handleMessage)

      this.eventSource.onerror = (e) => {
        console.error('[SSE-DEBUG] 연결 오류 발생:', e)
        this.disconnectSse()
        setTimeout(() => {
          if (authStore.isAuthenticated) {
            this.connectSse(callback)
          }
        }, 5000)
      }
    },

    disconnectSse() {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }
    }
  }
})
