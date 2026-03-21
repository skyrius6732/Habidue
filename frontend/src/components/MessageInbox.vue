<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick, computed } from 'vue'
import axios from '@/plugins/axios'
import { useMessageStore } from '@/stores/message'
import { useAuthStore } from '@/stores/auth'
import { format, isToday, isSameDay } from 'date-fns'
import { ko } from 'date-fns/locale'

const messageStore = useMessageStore()
const authStore = useAuthStore()

const selectedRoom = ref(null)
const conversationList = ref([])
const replyContent = ref('')
const replyFiles = ref([])
const chatScrollRef = ref(null)
const isSendingReply = ref(false)
const showMenu = ref(false)

// 접속 상태 갱신 인터벌
let activeStatusInterval = null

// 수정 모드 상태
const editingMessage = ref(null)

const currentMobileView = ref('LIST')

onMounted(async () => {
  await messageStore.fetchMessageRooms()
  await messageStore.fetchDailyStatus()
  
  activeStatusInterval = setInterval(async () => {
    try {
      await axios.post('/api/messages/active')
    } catch (e) {}
  }, 60000)
})

onUnmounted(() => {
  if (activeStatusInterval) clearInterval(activeStatusInterval)
})

const getPartnerId = (room) => {
  if (!room || room.isSystem) return null
  const myId = String(authStore.user?.id)
  const senderId = room.sender ? String(room.sender.id) : null
  return senderId === myId ? room.receiverId : room.sender?.id
}

const selectRoom = async (room) => {
  selectedRoom.value = room
  showMenu.value = false
  cancelEdit() 
  const partnerId = getPartnerId(room)
  
  if (partnerId) {
    conversationList.value = await messageStore.fetchConversation(partnerId)
    currentMobileView.value = 'CHAT'
    scrollToBottom()
    await messageStore.fetchMessageRooms()
  } else if (room.isSystem) {
    conversationList.value = [room]
    currentMobileView.value = 'CHAT'
    if (!room.isRead) {
      await messageStore.markAsRead(room.id)
      await messageStore.fetchMessageRooms()
    }
  }
}

const goBackToList = () => {
  currentMobileView.value = 'LIST'
  selectedRoom.value = null
  cancelEdit()
}

const processedConversation = computed(() => {
  if (conversationList.value.length === 0) return []
  const result = []
  if (selectedRoom.value && !selectedRoom.value.isSystem) {
    result.push({
      id: 'system-welcome',
      isSystem: true,
      content: `상대방에게 보내는 정중한 쪽지 한 통은 신뢰의 시작입니다.\n\nhabiDue는 건강한 소통을 위해 쪽지 발송 시 신뢰점수 0.1P를 사용하며, 하루 최대 20회까지 발송 가능합니다.\n당신의 따뜻한 한마디가 누군가에게는 큰 힘이 됩니다. 😊`
    })
  }
  let lastDate = null
  conversationList.value.forEach((msg) => {
    const msgDate = new Date(msg.createdAt)
    if (!lastDate || !isSameDay(lastDate, msgDate)) {
      result.push({
        id: `date-${msg.id}`,
        isDateHeader: true,
        content: format(msgDate, 'yyyy년 M월 d일', { locale: ko })
      })
      lastDate = msgDate
    }
    result.push(msg)
  })
  return result
})

const scrollToBottom = () => {
  nextTick(() => {
    if (chatScrollRef.value) {
      chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
    }
  })
}

const handleFileChange = (e) => {
  const files = Array.from(e.target.files)
  if (replyFiles.value.length + files.length > 5) {
    alert('파일은 최대 5개까지만 첨부할 수 있습니다.')
    return
  }
  replyFiles.value = [...replyFiles.value, ...files]
}

const removeFile = (idx) => { replyFiles.value.splice(idx, 1) }

const handleSendReply = async () => {
  if (!replyContent.value.trim() && replyFiles.value.length === 0) return
  if (!selectedRoom.value) return
  
  const partnerId = getPartnerId(selectedRoom.value)
  isSendingReply.value = true

  let success = false
  if (editingMessage.value) {
    success = await messageStore.editMessage(editingMessage.value.id, replyContent.value)
    if (success) cancelEdit()
  } else {
    const result = await messageStore.sendMessage(partnerId, replyContent.value, replyFiles.value)
    success = result.success
    if (!success) alert(result.message)
  }

  if (success) {
    replyContent.value = ''
    replyFiles.value = []
    conversationList.value = await messageStore.fetchConversation(partnerId)
    scrollToBottom()
    await messageStore.fetchMessageRooms()
  }
  isSendingReply.value = false
}

const startEdit = (msg) => {
  if (msg.isDeleted) return
  editingMessage.value = msg
  replyContent.value = msg.content
}

const cancelEdit = () => {
  editingMessage.value = null
  replyContent.value = ''
}

const handleDeleteMessage = async (msgId) => {
  if (!confirm('이 메시지를 삭제하시겠습니까?')) return
  const success = await messageStore.deleteMessage(msgId)
  if (success) {
    const partnerId = getPartnerId(selectedRoom.value)
    conversationList.value = await messageStore.fetchConversation(partnerId)
  }
}

const handleBlock = async () => {
  const partnerId = getPartnerId(selectedRoom.value)
  if (partnerId && confirm('이 사용자를 차단하시겠습니까?')) {
    const success = await messageStore.blockUser(partnerId)
    if (success) { alert('사용자가 차단되었습니다.'); showMenu.value = false }
  }
}

const handleReportRoom = async () => {
  if (confirm('이 대화방 상대를 신고하시겠습니까?')) {
    alert('신고가 접수되었습니다.'); showMenu.value = false
  }
}

const handleReportMsg = async (msg) => {
  if (confirm('이 메시지를 신고하시겠습니까?')) {
    const success = await messageStore.reportMessage(msg.id)
    if (success) { alert('신고가 접수되었습니다.') }
  }
}

const handleDeleteChat = async () => {
  const partnerId = getPartnerId(selectedRoom.value)
  if (partnerId && confirm('대화방을 나가시겠습니까? 삭제된 내역은 복구할 수 없습니다.')) {
    const success = await messageStore.deleteConversation(partnerId)
    if (success) {
      selectedRoom.value = null
      conversationList.value = []
      await messageStore.fetchMessageRooms()
      currentMobileView.value = 'LIST'
    }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isToday(date)) return format(date, 'HH:mm', { locale: ko })
  return format(date, 'M월 d일', { locale: ko })
}

const formatTimeOnly = (dateStr) => {
  if (!dateStr) return ''
  return format(new Date(dateStr), 'HH:mm', { locale: ko })
}

const isImage = (type) => type && type.startsWith('image/')
const openImage = (url) => window.open(url, '_blank')

const displayKarma = computed(() => {
  const points = authStore.user?.karmaPoint || 1000
  return (points / 10).toFixed(1)
})

const vClickOutside = {
  mounted(el, binding) {
    el.clickOutsideEvent = (event) => {
      if (!(el === event.target || el.contains(event.target))) {
        binding.value(event);
      }
    };
    document.addEventListener("click", el.clickOutsideEvent);
  },
  unmounted(el) {
    document.removeEventListener("click", el.clickOutsideEvent);
  },
};
</script>

<template>
  <div class="dm-container" :class="{ 'mobile-chat-active': currentMobileView === 'CHAT' }">
    <!-- 왼쪽: 사이드바 -->
    <div class="dm-sidebar" :class="{ 'mobile-hide': currentMobileView === 'CHAT' }">
      <div class="sidebar-header">
        <div class="sidebar-title-row">
          <h2 class="sidebar-title">Direct Message</h2>
          <div class="karma-badge-mini">
            <span class="karma-value">{{ displayKarma }} P</span>
          </div>
        </div>
      </div>
      
      <div v-if="messageStore.isLoading && messageStore.receivedMessages.length === 0" class="loading-state">
        <div class="spinner"></div>
      </div>
      
      <div v-else-if="messageStore.receivedMessages.length === 0" class="empty-list-sidebar">
        <div class="empty-icon-mini">📩</div>
        <p>메시지가 없습니다.</p>
      </div>
      
      <div v-else class="dm-list thin-scrollbar">
        <div 
          v-for="room in messageStore.receivedMessages" 
          :key="room.id" 
          class="dm-item"
          :class="{ 'active': String(getPartnerId(selectedRoom)) === String(getPartnerId(room)) }"
          @click="selectRoom(room)"
        >
          <div class="item-avatar">
            <span v-if="room.isSystem" class="system-icon">📢</span>
            <div v-else class="user-avatar-wrap">
              <div class="user-avatar-small">
                {{ (String(room.sender?.id) === String(authStore.user?.id) ? room.receiverNickname : room.sender?.nickname)?.[0] }}
              </div>
              <div v-if="!room.isSystem" class="online-indicator-dot" :class="{ 'is-online': room.isPartnerOnline }"></div>
            </div>
          </div>
          <div class="item-main">
            <div class="item-top">
              <div class="item-name-group">
                <span class="item-name">{{ room.isSystem ? '시스템' : (String(room.sender?.id) === String(authStore.user?.id) ? room.receiverNickname : room.sender?.nickname) }}</span>
                <div v-if="room.unreadCount > 0" class="unread-badge-circle">{{ room.unreadCount }}</div>
              </div>
              <span class="item-date">{{ formatDate(room.createdAt) }}</span>
            </div>
            <div class="item-bottom">
              <p class="item-preview" :class="{ 'unread-text': room.unreadCount > 0 }">
                {{ room.isDeleted ? '삭제된 메시지' : room.content }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 오른쪽: 메인 채팅창 -->
    <div class="dm-main" :class="{ 'mobile-show': currentMobileView === 'CHAT' }">
      <template v-if="selectedRoom">
        <div class="main-header">
          <div class="partner-meta">
            <button class="mobile-back-btn" @click="goBackToList">←</button>
            <div class="avatar-circle">
              {{ (selectedRoom.isSystem ? 'S' : (String(selectedRoom.sender?.id) === String(authStore.user?.id) ? selectedRoom.receiverNickname : selectedRoom.sender?.nickname)?.[0]) }}
            </div>
            <div class="name-box">
              <span class="partner-name">{{ selectedRoom.isSystem ? '시스템 메시지' : (String(selectedRoom.sender?.id) === String(authStore.user?.id) ? selectedRoom.receiverNickname : selectedRoom.sender?.nickname) }}</span>
              <span class="partner-status" :class="{ 'is-offline': !selectedRoom.isPartnerOnline }" v-if="!selectedRoom.isSystem">
                {{ selectedRoom.isPartnerOnline ? '활동 중' : '오프라인' }}
              </span>
            </div>
          </div>
          <div class="header-actions">
            <div class="menu-container" v-click-outside="() => showMenu = false">
              <button class="icon-btn" @click="showMenu = !showMenu">ⓘ</button>
              <div v-if="showMenu" class="dropdown-menu">
                <button v-if="!selectedRoom.isSystem" @click="handleDeleteChat" class="menu-item delete">대화방 삭제</button>
                <button v-if="!selectedRoom.isSystem" @click="handleBlock" class="menu-item block">사용자 차단</button>
                <button v-if="!selectedRoom.isSystem" @click="handleReportRoom" class="menu-item report">대화 상대 신고</button>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-area thin-scrollbar" ref="chatScrollRef">
          <div v-for="msg in processedConversation" :key="msg.id" class="msg-row-container">
            <div v-if="msg.isDateHeader" class="date-header"><span>{{ msg.content }}</span></div>
            <div v-else-if="msg.isSystem" class="system-guide-row"><div class="system-guide-bubble"><p>{{ msg.content }}</p></div></div>
            <div v-else-if="msg.isDeleted" class="deleted-msg-row"><div class="deleted-bubble">메시지가 삭제 되었습니다.</div></div>

            <div v-else class="msg-row" :class="{ 'msg-me': String(msg.sender?.id) === String(authStore.user?.id) }">
              <div class="msg-bubble-wrap">
                <div class="msg-bubble-group">
                  <div v-if="String(msg.sender?.id) === String(authStore.user?.id)" class="msg-side-meta">
                    <span class="msg-time-inline">{{ formatTimeOnly(msg.createdAt) }}</span>
                    <div class="msg-side-actions">
                      <button class="msg-action-btn" @click="startEdit(msg)" title="수정">✏️</button>
                      <button class="msg-action-btn" @click="handleDeleteMessage(msg.id)" title="삭제">✕</button>
                    </div>
                  </div>

                  <div class="msg-bubble">
                    <p class="msg-text">
                      {{ msg.content }}
                      <span v-if="msg.isEdited" class="edited-tag">(수정됨)</span>
                    </p>
                    <div v-if="msg.attachments && msg.attachments.length > 0" class="msg-attachments">
                      <div v-for="file in msg.attachments" :key="file.id" class="msg-file">
                        <img v-if="isImage(file.fileType)" :src="file.fileUrl" @click="openImage(file.fileUrl)" class="dm-img-thumb" />
                        <div v-else class="dm-file-box">
                          <span class="dm-f-name">{{ file.originalFileName }}</span>
                          <a :href="file.fileUrl" :download="file.originalFileName" class="dm-dl">📥</a>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div v-if="String(msg.sender?.id) !== String(authStore.user?.id)" class="msg-side-meta">
                    <span class="msg-time-inline">{{ formatTimeOnly(msg.createdAt) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 하단 입력 영역 -->
        <div class="input-container" v-if="!selectedRoom.isSystem">
          <div v-if="editingMessage" class="edit-preview-bar">
            <div class="edit-info">
              <span class="edit-label">메시지 수정 중</span>
              <p class="edit-text-preview">{{ editingMessage.content }}</p>
            </div>
            <button class="cancel-edit-btn" @click="cancelEdit">✕</button>
          </div>

          <div class="input-info-bar" v-else>
            <span class="limit-text">오늘 남은 발송: <strong>{{ messageStore.dailyStatus.remainingCount }}</strong> / {{ messageStore.dailyStatus.maxCount }}</span>
            <span class="cost-text">신뢰점수 0.1P 소모</span>
          </div>
          
          <div v-if="replyFiles.length > 0 && !editingMessage" class="f-previews thin-scrollbar">
            <div v-for="(f, i) in replyFiles" :key="i" class="f-chip">
              <span class="f-name">{{ f.name }}</span>
              <button @click="removeFile(i)" class="f-remove">&times;</button>
            </div>
          </div>

          <div class="input-form-wrapper" :class="{ 'editing-active': editingMessage }">
            <div class="input-field-container">
              <label v-if="!editingMessage" class="attach-trigger"><input type="file" multiple @change="handleFileChange" style="display: none;">📎</label>
              <textarea v-model="replyContent" :placeholder="editingMessage ? '메시지 수정...' : '메시지 보내기...'" @keydown.enter.exact.prevent="handleSendReply" :disabled="isSendingReply" rows="3"></textarea>
            </div>
            <div class="input-footer-actions">
              <button class="real-send-btn" @click="handleSendReply" :disabled="isSendingReply || (!replyContent.trim() && replyFiles.length === 0)" :class="{ 'active': replyContent.trim() || replyFiles.length > 0 }">
                {{ editingMessage ? '수정 완료' : '전송' }}
              </button>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="main-empty">
        <div class="dm-large-icon">✉️</div>
        <h2>내 메시지</h2>
        <p>친구나 커뮤니티 멤버에게 메시지를 보내보세요.</p>
        <button class="primary-btn" @click="messageStore.fetchMessageRooms()">새 메시지 확인</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dm-container { display: flex; height: 750px; background: var(--card-bg); border-radius: 16px; border: 1px solid var(--border-color); overflow: hidden; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1); }
.thin-scrollbar::-webkit-scrollbar { width: 4px; height: 4px; }
.thin-scrollbar::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 10px; }

/* 사이드바 헤더 & 신뢰점수 위치 */
.dm-sidebar { width: 350px; flex-shrink: 0; border-right: 1px solid var(--border-color); display: flex; flex-direction: column; background: var(--bg-primary); }
.sidebar-header { padding: 24px 20px; border-bottom: 1px solid var(--border-color); }
.sidebar-title-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.sidebar-title { font-size: 1.2rem; font-weight: 950; margin: 0; letter-spacing: -0.8px; white-space: nowrap; }
.karma-badge-mini { background: rgba(168, 85, 247, 0.1); padding: 2px 8px; border-radius: 12px; border: 1px solid rgba(168, 85, 247, 0.2); }
.karma-value { font-size: 0.75rem; font-weight: 900; color: #a855f7; }

.dm-list { flex: 1; overflow-y: auto; }
.dm-item { padding: 16px 20px; display: flex; align-items: center; gap: 14px; cursor: pointer; transition: 0.2s; position: relative; }
.dm-item:hover { background: var(--hover-bg); }
.dm-item.active { background: var(--hover-bg); }
.dm-item.active::after { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: #a855f7; }

/* 아바타 및 온라인 표시 */
.user-avatar-wrap { position: relative; }
.user-avatar-small { width: 52px; height: 52px; border-radius: 50%; background: linear-gradient(135deg, #c084fc, #6366f1); color: white; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 1.3rem; }
.online-indicator-dot { position: absolute; bottom: 2px; right: 2px; width: 14px; height: 14px; border-radius: 50%; background: #94a3b8; border: 2px solid var(--bg-primary); }
.online-indicator-dot.is-online { background: #10b981; }

/* 리스트 아이템 텍스트 */
.item-main { flex: 1; min-width: 0; }
.item-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.item-name-group { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
.item-name { font-weight: 800; font-size: 0.95rem; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.unread-badge-circle { background: #ef4444; color: white; font-size: 0.65rem; font-weight: 900; width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; line-height: 1; }
.item-date { font-size: 0.75rem; color: var(--text-muted); flex-shrink: 0; margin-left: 10px; }
.item-preview { font-size: 0.85rem; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0; }
.unread-text { color: var(--text-primary); font-weight: 700; }

/* 메인 채팅창 */
.dm-main { flex: 1; min-width: 0; display: flex; flex-direction: column; background: var(--card-bg); position: relative; }
.main-header { padding: 15px 20px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; background: var(--card-bg); backdrop-filter: blur(15px); z-index: 50; }
.partner-meta { display: flex; align-items: center; gap: 12px; }
.mobile-back-btn { display: none; }
.avatar-circle { width: 40px; height: 40px; border-radius: 50%; background: var(--hover-bg); display: flex; align-items: center; justify-content: center; font-weight: 900; color: #a855f7; border: 1px solid var(--border-color); }
.partner-name { font-weight: 850; font-size: 1.1rem; color: var(--text-primary); }
.partner-status { font-size: 0.75rem; color: #10b981; font-weight: 700; margin-top: 2px; display: block; }
.partner-status.is-offline { color: var(--text-muted); }

/* 메뉴 */
.icon-btn { background: none; border: none; font-size: 1.4rem; cursor: pointer; color: var(--text-muted); transition: 0.2s; padding: 5px; }
.dropdown-menu { position: absolute; right: 0; top: 100%; width: 170px; background: var(--card-bg); border: 1.5px solid var(--border-color); border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.15); z-index: 100; overflow: hidden; }
.menu-item { width: 100%; padding: 12px 16px; border: none; background: none; text-align: left; font-size: 0.9rem; font-weight: 700; cursor: pointer; color: var(--text-primary); transition: 0.2s; }
.menu-item:hover { background: var(--hover-bg); }
.menu-item.delete { border-bottom: 1px solid var(--border-color); }
.menu-item.block { color: #e67e22; }
.menu-item.report { color: #ef4444; }

/* 채팅 영역 */
.chat-area { flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 15px; }
.date-header { display: flex; justify-content: center; margin: 25px 0; position: relative; }
.date-header span { background: var(--card-bg); padding: 5px 15px; border-radius: 20px; font-size: 0.7rem; color: var(--text-muted); font-weight: 800; border: 1px solid var(--border-color); z-index: 1; }
.system-guide-bubble { max-width: 85%; background: var(--bg-primary); border: 1px dashed var(--border-color); padding: 15px; border-radius: 15px; margin: 0 auto 20px; }
.system-guide-bubble p { font-size: 0.8rem; color: var(--text-secondary); line-height: 1.6; margin: 0; white-space: pre-wrap; text-align: center; }
.deleted-msg-row { display: flex; justify-content: center; margin: 15px 0; width: 100%; }
.deleted-bubble { background: var(--bg-primary); color: var(--text-muted); font-size: 0.8rem; padding: 10px 30px; border-radius: 25px; border: 1.5px dashed var(--border-color); font-weight: 600; font-style: italic; }

/* 말풍선 & 시간/액션 */
.msg-row { display: flex; width: 100%; }
.msg-me { justify-content: flex-end; }
.msg-bubble-wrap { max-width: 75%; display: flex; flex-direction: column; gap: 4px; }
.msg-bubble-group { display: flex; align-items: flex-end; gap: 12px; }
.msg-bubble { padding: 12px 16px; border-radius: 20px; position: relative; box-shadow: 0 2px 8px rgba(0,0,0,0.03); min-width: 40px; }
.msg-me .msg-bubble { background: linear-gradient(135deg, #a855f7, #6366f1); color: white; border-bottom-right-radius: 4px; }
.msg-row:not(.msg-me) .msg-bubble { background: var(--hover-bg); color: var(--text-primary); border: 1px solid var(--border-color); border-bottom-left-radius: 4px; }

.msg-side-meta { display: flex; flex-direction: column; justify-content: flex-end; min-width: 50px; height: 100%; padding-bottom: 4px; }
.msg-me .msg-side-meta { align-items: flex-end; }
.msg-time-inline { font-size: 0.6rem; color: var(--text-muted); font-weight: 600; }
.msg-side-actions { display: none; gap: 6px; }
.msg-action-btn { background: var(--bg-primary); border: 1px solid var(--border-color); width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.7rem; cursor: pointer; transition: 0.2s; }
.msg-action-btn:hover { background: var(--hover-bg); transform: scale(1.1); color: #a855f7; }
.msg-row:hover .msg-side-actions { display: flex; }
.edited-tag { font-size: 0.6rem; opacity: 0.7; margin-left: 4px; font-weight: 400; }

/* 수정 프리뷰 바 */
.edit-preview-bar { 
  background: var(--bg-primary); 
  border: 1.5px solid #a855f7;
  border-bottom: none;
  padding: 10px 15px; 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  border-radius: 20px 20px 0 0;
  margin-bottom: 0;
}
.edit-info { flex: 1; min-width: 0; }
.edit-label { font-size: 0.65rem; font-weight: 800; color: #a855f7; display: block; margin-bottom: 2px; }
.edit-text-preview { font-size: 0.8rem; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0; }
.cancel-edit-btn { background: none; border: none; font-size: 1rem; color: var(--text-muted); cursor: pointer; padding: 5px; display: flex; align-items: center; justify-content: center; }
.cancel-edit-btn:hover { color: #ef4444; }

/* 입력 폼 & 파일 칩 */

.input-container { padding: 15px 20px; border-top: 1px solid var(--border-color); }
.input-info-bar { display: flex; justify-content: space-between; align-items: center; font-size: 0.75rem; color: var(--text-secondary); margin-bottom: 12px; padding: 0 5px; }
.limit-text strong { color: #a855f7; font-weight: 900; }
.cost-text { background: rgba(168, 85, 247, 0.1); color: #a855f7; padding: 3px 10px; border-radius: 6px; font-weight: 800; font-size: 0.7rem; }

.f-previews { display: flex; gap: 8px; margin-bottom: 12px; overflow-x: auto; padding-bottom: 5px; }
.f-chip { background: var(--hover-bg); border: 1px solid var(--border-color); padding: 6px 12px; border-radius: 20px; display: flex; align-items: center; gap: 8px; flex-shrink: 0; box-shadow: 0 2px 5px rgba(0,0,0,0.03); }
.f-name { font-size: 0.75rem; font-weight: 600; color: var(--text-primary); max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.f-remove { background: var(--border-color); border: none; color: var(--text-muted); width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 0.8rem; transition: 0.2s; }
.f-remove:hover { background: #ef4444; color: white; }

.input-form-wrapper { background: var(--hover-bg); border-radius: 20px; border: 1px solid var(--border-color); padding: 10px 16px; display: flex; flex-direction: column; }
.input-form-wrapper.editing-active { border-radius: 0 0 20px 20px; border-top: none; background: var(--card-bg); border-color: #a855f7; }
.input-form-wrapper:focus-within { border-color: #a855f7; background: var(--card-bg); }
.input-field-container { display: flex; gap: 12px; align-items: flex-start; width: 100%; }
.attach-trigger { font-size: 1.4rem; cursor: pointer; padding-top: 8px; filter: grayscale(1); opacity: 0.6; }
textarea { flex: 1; width: 100%; border: none; background: transparent; padding: 8px 0; resize: none; font-size: 0.95rem; color: var(--text-primary); outline: none; min-height: 45px; }
textarea::-webkit-scrollbar { width: 3px; }
textarea::-webkit-scrollbar-thumb { background: rgba(168, 85, 247, 0.2); border-radius: 10px; }

.input-footer-actions { 
  display: flex; 
  justify-content: flex-end; 
  width: 100%; 
  margin-top: 4px;
}
.real-send-btn { 
  background: none; 
  border: none; 
  color: #a855f7; 
  font-weight: 950; 
  cursor: pointer; 
  opacity: 0.3; 
  transition: 0.2s; 
  padding: 5px 10px; 
}
.real-send-btn.active { opacity: 1; transform: scale(1.05); }

/* 이미지 썸네일 */
.dm-img-thumb { max-width: 200px; max-height: 200px; border-radius: 12px; margin-top: 8px; cursor: pointer; object-fit: cover; border: 1px solid var(--border-color); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

/* 엠프티 상태 */
.empty-list-sidebar { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; text-align: center; color: var(--text-muted); }
.empty-icon-mini { font-size: 2.5rem; margin-bottom: 15px; opacity: 0.6; }
.main-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 40px; background: var(--card-bg); }
.dm-large-icon { font-size: 5rem; margin-bottom: 25px; background: var(--hover-bg); width: 120px; height: 120px; display: flex; align-items: center; justify-content: center; border-radius: 50%; border: 1px solid var(--border-color); }
.primary-btn { background: linear-gradient(135deg, #a855f7, #6366f1); color: white; border: none; padding: 12px 28px; border-radius: 10px; font-weight: 800; cursor: pointer; box-shadow: 0 4px 15px rgba(168, 85, 247, 0.25); }

@media (max-width: 768px) {
  .dm-container { height: 85vh; border-radius: 0; border: none; }
  .dm-sidebar { width: 100%; border-right: none; } /* 우측 선 제거 */
  .dm-sidebar.mobile-hide { display: none; }
  .dm-main { display: none; width: 100%; }
  .dm-main.mobile-show { display: flex; }
  .mobile-back-btn { display: block; background: none; border: none; font-size: 1.5rem; margin-right: 10px; color: var(--text-primary); }

  /* 모바일 대화 목록 아이템 축소 */
  .dm-item { padding: 10px 12px; gap: 10px; }
  .user-avatar-small { width: 38px; height: 38px; font-size: 1rem; }
  .online-indicator-dot { width: 10px; height: 10px; bottom: 1px; right: 1px; border-width: 1.5px; }
  
  .item-name { font-size: 0.85rem; }
  .unread-badge-circle { width: 15px; height: 18px; font-size: 0.6rem; min-width: 15px; }
  .item-date { font-size: 0.65rem; margin-left: 6px; }
  .item-preview { font-size: 0.75rem; }
  
  .sidebar-header { padding: 15px; }
  .sidebar-title { font-size: 1.1rem; }
  .karma-badge-mini { padding: 1px 6px; }
  .karma-value { font-size: 0.65rem; }

  /* 모바일 채팅창 내부 요소 최적화 */
  .msg-bubble { padding: 8px 12px; border-radius: 14px; }
  .msg-text { font-size: 0.8rem; line-height: 1.4; } /* 텍스트 크기 축소 */
  .msg-time-inline { font-size: 0.5rem; }
  .msg-side-meta { min-width: 35px; }
  
  .dm-img-thumb { max-width: 100px; max-height: 130px; } /* 초소형 화면 대응: 100px로 축소 */
  
  .avatar-circle { width: 32px; height: 32px; font-size: 0.8rem; }
  .partner-name { font-size: 0.95rem; }
  
  .system-guide-bubble { padding: 10px; max-width: 90%; }
  .system-guide-bubble p { font-size: 0.7rem; }
}
</style>
