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
      let serverData = res.data?.data?.content || []
      
      // [UI 테스트용 더미 데이터 결합]
      const dummyRooms = [
        { id: 999, sender: { id: 101, nickname: 'Instagram_User' }, receiverId: 1, content: '오늘 날씨가 정말 좋네요! 사진 보셨나요?', unreadCount: 3, isPartnerOnline: true, createdAt: new Date().toISOString() },
        { id: 998, sender: { id: 102, nickname: 'HabiDue_Lover' }, receiverId: 1, content: '네, 알겠습니다. 내일 뵙겠습니다.', unreadCount: 0, isPartnerOnline: false, createdAt: '2026-03-20T10:00:00' },
        { id: 997, isSystem: true, content: '커뮤니티 가이드라인을 준수해 주세요.', unreadCount: 1, createdAt: '2026-03-19T15:30:00' },
        { id: 996, sender: { id: 103, nickname: '긴닉네임을가진테스트유저입니다안녕하세요' }, receiverId: 1, content: '매우 긴 메시지 미리보기 테스트입니다. 이 메시지가 말줄임표 처리가 잘 되는지 확인해 보세요.', unreadCount: 15, isPartnerOnline: true, createdAt: new Date().toISOString() },
        { id: 995, sender: { id: 104, nickname: '디자인체크' }, receiverId: 1, content: '📎 사진을 보냈습니다.', unreadCount: 0, isPartnerOnline: true, createdAt: '2026-03-21T09:00:00' },
        { id: 994, sender: { id: 105, nickname: '오프라인친구' }, receiverId: 1, content: '자니?', unreadCount: 1, isPartnerOnline: false, createdAt: '2026-03-21T01:00:00' },
        { id: 993, sender: { id: 106, nickname: '새로운멤버' }, receiverId: 1, content: '만나서 반가워요!', unreadCount: 0, isPartnerOnline: true, createdAt: '2026-03-21T11:30:00' }
      ]

      receivedMessages.value = [...serverData, ...dummyRooms]
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
