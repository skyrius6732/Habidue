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
    if (isLoading.value) return currentConversation.value
    isLoading.value = true
    try {
      const res = await axios.get(`/api/messages/conversation/${partnerId}`)
      currentConversation.value = res.data.data
      return currentConversation.value
    } catch (e) {
      console.error('대화 내역 로드 실패:', e)
      return []
    } finally {
      isLoading.value = false
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
      // 서버에서 보내준 에러 메시지가 있으면 우선 사용, 없으면 기본 메시지 사용
      const msg = e.response?.data?.message || e.response?.data || '쪽지 발송에 실패했습니다.'
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

  const markRoomAsRead = async (partnerId) => {
    try {
      await axios.patch(`/api/messages/conversation/${partnerId}/read`)
      // 로컬 목록에서도 안읽음 숫자 초기화
      const room = receivedMessages.value.find(m => {
        const pId = (m.sender && m.sender.id === partnerId) || (m.receiverId === partnerId);
        return pId;
      });
      if (room) {
        room.unreadCount = 0;
        updateUnreadCount();
      }
    } catch (e) {
      console.error('대화방 읽음 처리 실패:', e)
    }
  }

  const reportMessage = async (messageId, reason = '부적절한 내용의 쪽지') => {
    try {
      await axios.post(`/api/messages/${messageId}/report`, { reason })
      return { success: true }
    } catch (e) {
      console.error('쪽지 신고 실패:', e.response?.data || e.message)
      // [시니어 조치] 서버 ApiResponse 구조(data.message)를 정확히 파싱
      const errorMsg = e.response?.data?.message || e.message || '신고 접수 중 오류가 발생했습니다.'
      return { success: false, message: errorMsg }
    }
  }

  const blockUser = async (userId) => {
    try { await axios.post(`/api/messages/block/${userId}`); return true } catch (e) { return false }
  }

  // 차단 해제
  const unblockUser = async (userId) => {
    try {
      await axios.delete(`/api/messages/block/${userId}`)
      return true
    } catch (e) {
      console.error('차단 해제 실패:', e)
      return false
    }
  }

  // 차단 목록 조회
  const fetchBlockedUsers = async () => {
    try {
      const res = await axios.get('/api/messages/block/list')
      return res.data.data
    } catch (e) {
      console.error('차단 목록 로드 실패:', e)
      return []
    }
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
    fetchMessageRooms, fetchReceivedMessages, fetchConversation, sendMessage, markAsRead, markRoomAsRead, reportMessage, blockUser, unblockUser, fetchBlockedUsers, fetchDailyStatus, deleteConversation, deleteMessage, editMessage
  }
})
