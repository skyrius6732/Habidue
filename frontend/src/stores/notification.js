import { defineStore } from 'pinia'
import axios from '@/plugins/axios'
import { useAuthStore } from './auth'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    unreadCount: 0,
    eventSource: null
  }),

  actions: {
    async fetchNotifications() {
      try {
        const res = await axios.get('/api/notifications')
        this.notifications = res.data.data
        this.unreadCount = this.notifications.filter(n => !n.isRead).length
      } catch (e) {
        console.error('알림 로드 실패', e)
      }
    },

    async fetchUnreadCount() {
      try {
        const res = await axios.get('/api/notifications/unread-count')
        this.unreadCount = res.data.data
      } catch (e) {}
    },

    async markAsRead(id) {
      const noti = this.notifications.find(n => n.id === id)
      if (noti && !noti.isRead) {
        noti.isRead = true
        this.unreadCount = Math.max(0, this.unreadCount - 1)
        try {
          await axios.patch(`/api/notifications/${id}/read`)
        } catch (e) {
          console.error('읽음 처리 실패', e)
        }
      }
    },

    async markAllAsRead() {
      this.notifications.forEach(n => n.isRead = true)
      this.unreadCount = 0
      try {
        await axios.patch('/api/notifications/read-all')
      } catch (e) {
        console.error('전체 읽음 처리 실패', e)
        await this.fetchNotifications()
      }
    },

    // SSE 실시간 연결 (기본 메시지 핸들러 사용)
    connectSse(callback) {
      const authStore = useAuthStore()
      if (!authStore.isAuthenticated || this.eventSource) return

      const token = authStore.accessToken
      this.eventSource = new EventSource(`/api/notifications/subscribe?token=${token}`)

      // [시니어 조치] 특정 이벤트명 리스너 대신 기본 onmessage 사용 (호환성)
      this.eventSource.onmessage = (event) => {
        try {
          const newNoti = JSON.parse(event.data)
          if (!this.notifications.some(n => n.id === newNoti.id)) {
            this.notifications.unshift(newNoti)
            this.unreadCount++
            if (callback) callback(newNoti)
          }
        } catch (e) {
          console.error('[SSE] Data parse error', e)
        }
      }

      this.eventSource.onerror = (e) => {
        console.error('[SSE] Connection error, retrying...', e)
        this.disconnectSse()
        // 5초 후 재연결 시도
        setTimeout(() => this.connectSse(callback), 5000)
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
