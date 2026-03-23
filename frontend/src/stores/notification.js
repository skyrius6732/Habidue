import { defineStore } from 'pinia'
import axios from '@/plugins/axios'
import { useAuthStore } from './auth'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    unreadCount: 0,
    eventSource: null,
    isMarkingAll: false
  }),

  actions: {
    // [시니어 조치] 서버 데이터 정규화 및 캐시 무효화
    async fetchNotifications() {
      if (this.isMarkingAll) return 
      try {
        const res = await axios.get(`/api/notifications?_t=${Date.now()}`)
        // [시니어 조치] Jackson 직렬화 변수명(isRead -> read) 변경 가능성 완벽 대비
        this.notifications = res.data.data.map(n => ({
          ...n,
          isRead: n.isRead === true || n.read === true || n.isRead === 1 || n.read === 1 || n.isRead === 'true' || n.read === 'true'
        }))
        this.syncUnreadCount()
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

    // 로컬 카운트 동기화 유틸
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
      
      // 로컬 즉시 반영
      this.notifications = this.notifications.map(n => ({ ...n, isRead: true }))
      this.unreadCount = 0
      
      try {
        await axios.patch('/api/notifications/read-all')
        // [시니어 조치] 서버 반영 보장을 위해 약간의 지연 후 상태 해제
        setTimeout(() => { this.isMarkingAll = false }, 1000)
      } catch (e) {
        this.isMarkingAll = false
        await this.fetchNotifications()
      }
    },

    connectSse(callback) {
      const authStore = useAuthStore()
      if (!authStore.isAuthenticated || !authStore.accessToken || this.eventSource) return

      const token = authStore.accessToken
      this.eventSource = new EventSource(`/api/notifications/subscribe?token=${token}`)

      this.eventSource.onmessage = (event) => {
        try {
          const rawData = JSON.parse(event.data)
          // SSE 데이터도 정규화
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
          console.error('[SSE] Parse error', e)
        }
      }

      this.eventSource.onerror = (e) => {
        this.disconnectSse()
        if (authStore.isAuthenticated && authStore.accessToken) {
          setTimeout(() => this.connectSse(callback), 15000)
        }
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
