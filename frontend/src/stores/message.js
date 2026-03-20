import { defineStore } from 'pinia'
import axios from '@/plugins/axios'
import { ref } from 'vue'

export const useMessageStore = defineStore('message', () => {
  const receivedMessages = ref([])
  const unreadCount = ref(0)
  const isLoading = ref(false)

  // 수신 쪽지함 조회
  const fetchReceivedMessages = async (page = 0, size = 20) => {
    isLoading.value = true
    try {
      const res = await axios.get(`/api/messages/received?page=${page}&size=${size}`)
      if (res.data && res.data.data && res.data.data.content) {
        receivedMessages.value = res.data.data.content
        // 서버에서 온 데이터의 isRead 상태를 그대로 반영하여 카운트 계산
        updateUnreadCount()
      }
    } catch (e) {
      console.error('쪽지 목록 로드 실패:', e)
    } finally {
      isLoading.value = false
    }
  }

  // 안읽은 쪽지 수 수동 업데이트
  const updateUnreadCount = () => {
    // console.log('전체 쪽지 상태:', receivedMessages.value.map(m => m.isRead)); // 디버깅용
    unreadCount.value = receivedMessages.value.filter(m => m.isRead === false).length
  }

  // 쪽지 보내기 (파일 첨부 지원)
  const sendMessage = async (receiverId, content, files = []) => {
    try {
      const formData = new FormData()
      formData.append('receiverId', receiverId)
      formData.append('content', content)
      
      if (files && files.length > 0) {
        files.forEach(file => {
          formData.append('files', file)
        })
      }

      await axios.post('/api/messages', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      return { success: true }
    } catch (e) {
      const msg = e.response?.data?.message || '쪽지 발송에 실패했습니다.'
      return { success: false, message: msg }
    }
  }

  // 쪽지 읽음 처리
  const markAsRead = async (messageId) => {
    try {
      await axios.patch(`/api/messages/${messageId}/read`)
      // 로컬 상태 즉시 업데이트 (서버 다시 안 불러와도 되게)
      const msg = receivedMessages.value.find(m => m.id === messageId)
      if (msg && !msg.isRead) {
        msg.isRead = true
        updateUnreadCount()
      }
    } catch (e) {
      console.error('쪽지 읽음 처리 실패:', e)
    }
  }

  // 쪽지 신고
  const reportMessage = async (messageId) => {
    try {
      await axios.post(`/api/messages/${messageId}/report`)
      return true
    } catch (e) {
      console.error('쪽지 신고 실패:', e)
      return false
    }
  }

  // 유저 차단
  const blockUser = async (userId) => {
    try {
      await axios.post(`/api/messages/block/${userId}`)
      return true
    } catch (e) {
      console.error('유저 차단 실패:', e)
      return false
    }
  }

  return { 
    receivedMessages, unreadCount, isLoading,
    fetchReceivedMessages, sendMessage, markAsRead, reportMessage, blockUser 
  }
})
