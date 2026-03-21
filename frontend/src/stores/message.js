import { defineStore } from 'pinia'
import axios from '@/plugins/axios'
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

export const useMessageStore = defineStore('message', () => {
  const receivedMessages = ref([]) 
  const currentConversation = ref([]) 
  const unreadCount = ref(0)
  const isLoading = ref(false)
  const dailyStatus = ref({ currentCount: 0, maxCount: 20, remainingCount: 20 })

  const fetchMessageRooms = async (page = 0, size = 20) => {
    isLoading.value = true
    try {
      const res = await axios.get(`/api/messages/rooms?page=${page}&size=${size}`)
      receivedMessages.value = res.data?.data?.content || []
      updateUnreadCount()
    } catch (e) {
      console.error('대화방 목록 로드 실패:', e)
    } finally {
      isLoading.value = false
    }
  }

  const fetchReceivedMessages = fetchMessageRooms;

  const fetchDailyStatus = async () => {
    try {
      const res = await axios.get('/api/messages/status')
      dailyStatus.value = res.data.data
      return dailyStatus.value
    } catch (e) { return dailyStatus.value }
  }

  const fetchConversation = async (partnerId) => {
    try {
      const res = await axios.get(`/api/messages/conversation/${partnerId}`)
      currentConversation.value = res.data.data
      return currentConversation.value
    } catch (e) {
      console.error('대화 내역 로드 실패:', e)
      return []
    }
  }

  const updateUnreadCount = () => {
    unreadCount.value = receivedMessages.value.filter(m => m.unreadCount > 0).length
  }

  const sendMessage = async (receiverId, content, files = []) => {
    if (!receiverId) return { success: false, message: '수신자 정보가 없습니다.' }
    
    try {
      const formData = new FormData()
      formData.append('content', content || '')
      
      if (files && files.length > 0) {
        files.forEach(file => {
          formData.append('files', file)
        })
      }

      const res = await axios.post(`/api/messages/${receiverId}`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      
      await fetchDailyStatus() 
      // [시니어 조치] 발송 후 카르마 즉시 갱신을 위해 프로필 정보 다시 가져옴
      const authStore = useAuthStore()
      await authStore.fetchUserProfile()
      
      return { success: true }
    } catch (e) {
      console.error('쪽지 전송 실패:', e.response?.data || e.message)
      const msg = e.response?.data?.message || '쪽지 발송에 실패했습니다.'
      return { success: false, message: msg }
    }
  }

  const markAsRead = async (messageId) => {
    try {
      await axios.patch(`/api/messages/${messageId}/read`)
      const msg = receivedMessages.value.find(m => m.id === messageId)
      if (msg) { msg.isRead = true; updateUnreadCount() }
    } catch (e) {}
  }

  const reportMessage = async (messageId) => {
    try { await axios.post(`/api/messages/${messageId}/report`); return true } catch (e) { return false }
  }

  const blockUser = async (userId) => {
    try { await axios.post(`/api/messages/block/${userId}`); return true } catch (e) { return false }
  }

  const deleteConversation = async (partnerId) => {
    try { await axios.delete(`/api/messages/conversation/${partnerId}`); return true } catch (e) { return false }
  }

  // 개별 메시지 삭제
  const deleteMessage = async (messageId) => {
    try {
      await axios.delete(`/api/messages/${messageId}`)
      return true
    } catch (e) {
      console.error('쪽지 삭제 실패:', e)
      return false
    }
  }

  // 개별 메시지 수정 (JSON 전송 방식)
  const editMessage = async (messageId, content) => {
    try {
      await axios.patch(`/api/messages/${messageId}/edit`, { content })
      return true
    } catch (e) {
      console.error('쪽지 수정 실패:', e)
      return false
    }
  }

  return { 
    receivedMessages, currentConversation, unreadCount, isLoading, dailyStatus,
    fetchMessageRooms, fetchReceivedMessages, fetchConversation, sendMessage, markAsRead, reportMessage, blockUser, fetchDailyStatus, deleteConversation, deleteMessage, editMessage
  }
})
