<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/plugins/axios'
import { useMessageStore } from '@/stores/message'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { format, isToday, isSameDay } from 'date-fns'
import { ko } from 'date-fns/locale'

const route = useRoute()
const messageStore = useMessageStore()
const authStore = useAuthStore()
const uiStore = useUiStore()

const selectedRoom = ref(null)
const conversationList = ref([])
const replyContent = ref('')
const replyFiles = ref([])
const chatScrollRef = ref(null)
const isSendingReply = ref(false)
const showMenu = ref(false)

// 차단 관리 관련 상태
const showBlockedListModal = ref(false)
const blockedUsers = ref([])

// 접속 상태 갱신 인터벌
let activeStatusInterval = null

// 수정 모드 상태
const editingMessage = ref(null)

const currentMobileView = ref('LIST')

// [시니어 조치] 데이터 로드 로직 통합
const loadInitialData = async () => {
  try {
    await Promise.all([
      authStore.fetchUserProfile(),
      messageStore.fetchMessageRooms(),
      messageStore.fetchDailyStatus()
    ]);
  } catch (e) {
    console.error('초기 데이터 로드 실패', e);
  }
}

// [시니어 조치] 라우트 쿼리(타임스탬프 등) 변화 감시하여 즉시 새로고침
watch(() => route.query, (newQuery) => {
  if (newQuery.tab === 'messages') {
    messageStore.fetchMessageRooms()
  }
}, { deep: true })

onMounted(async () => {
  await loadInitialData()
  
  activeStatusInterval = setInterval(async () => {
    try {
      await axios.post('/api/messages/active')
    } catch (e) {}
  }, 60000)
})

onUnmounted(() => {
  if (activeStatusInterval) clearInterval(activeStatusInterval)
})

const myId = String(authStore.user?.publicId)

const isMe = (userPublicId) => {
  if (!myId || !userPublicId) return false
  return String(myId) === String(userPublicId)
}

const getPartnerId = (room) => {
  if (!room) return null
  const myPublicId = String(authStore.user?.publicId)

  // [시니어 조치] 공개 ID(publicId)만 사용 (BackEnd와 일관성)
  const senderPublicId = room.sender?.publicId || room.senderPublicId || null
  const receiverPublicId = room.receiver?.publicId || room.receiverPublicId || null

  // 공개 ID 기반 식별만 수행
  if (senderPublicId && String(senderPublicId) !== myPublicId) return String(senderPublicId)
  if (receiverPublicId && String(receiverPublicId) !== myPublicId) return String(receiverPublicId)

  return null
}

const selectRoom = async (room) => {
  selectedRoom.value = room
  showMenu.value = false
  cancelEdit() 
  const partnerId = getPartnerId(room)
  
  if (partnerId) {
    // [시니어 조치] 대화방 진입 시 읽음 처리 즉시 수행 (배지 제거)
    await messageStore.markRoomAsRead(partnerId)
    
    conversationList.value = await messageStore.fetchConversation(partnerId)
    currentMobileView.value = 'CHAT'
    scrollToBottom()
    await messageStore.fetchMessageRooms()
  } else {
    // 파트너 ID가 없는 순수 시스템 공지방인 경우
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

// [시니어 조치] 특정 메시지로 스크롤 이동 및 하이라이트
const scrollToMessage = (msgId) => {
  if (!msgId) return
  nextTick(() => {
    const rowEl = document.getElementById(`msg-${msgId}`)
    if (rowEl) {
      rowEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
      const bubbleEl = rowEl.querySelector('.msg-bubble')
      if (bubbleEl) {
        bubbleEl.classList.add('highlight-flash')
        setTimeout(() => bubbleEl.classList.remove('highlight-flash'), 2000)
      }
    } else {
      uiStore.showAlert('해당 메시지를 찾을 수 없습니다.', '알림')
    }
  })
}

// [시니어 조치] 대화방의 발송 제한 여부 계산 (가장 최신 시스템 메시지 기준)
const isRoomRestricted = computed(() => {
  if (conversationList.value.length === 0) return false
  const restrictionMsgs = conversationList.value.filter(m => m.isSystem && m.isRoomRestricted !== undefined)
  if (restrictionMsgs.length === 0) return false
  // 마지막 시스템 메시지가 제한 상태인지 확인
  return restrictionMsgs[restrictionMsgs.length - 1].isRoomRestricted
})

// [시니어 조치] 상대방이 탈퇴했는지 확인
const isPartnerWithdrawn = computed(() => {
  if (!selectedRoom.value || selectedRoom.value.isSystem) return false
  
  // 1. 백엔드 응답 필드 확인
  if (selectedRoom.value.isPartnerWithdrawn) return true
  
  // 2. 닉네임이 마스킹 처리되었는지 확인
  const partnerNickname = isMe(selectedRoom.value.sender?.publicId) 
    ? selectedRoom.value.receiverNickname 
    : selectedRoom.value.sender?.nickname;
    
  if (partnerNickname === '(탈퇴한 사용자)') return true;

  // 3. ID가 없는 경우 확인
  const partnerId = getPartnerId(selectedRoom.value)
  return !partnerId
})

// [시니어 조치] 날짜 헤더 표시 및 권한 필터링 로직 (시간순 정렬 복구)
const processedConversation = computed(() => {
  if (conversationList.value.length === 0) return []
  
  const currentUserId = String(authStore.user?.id)

  // 1. 가시성 필터링 및 시간순 정렬 (과거순 ASC)
  const visibleMessages = [...conversationList.value]
    .filter(msg => {
      if (!msg.visibleToUserId) return true
      return String(msg.visibleToUserId) === currentUserId
    })
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt)) // 과거순 정렬 복구

  const result = []
  
  // 2. 맨 처음에 시스템 환영 안내 메시지 삽입 (방이 제한되지 않았을 때만)
  if (selectedRoom.value && !selectedRoom.value.isSystem && !isRoomRestricted.value) {
    result.push({
      id: 'system-welcome',
      isSystem: true,
      content: `상대방에게 보내는 정중한 쪽지 한 통은 신뢰의 시작입니다.\n\nhabiDue는 건강한 소통을 위해 쪽지 발송 시 신뢰점수 0.1P를 사용하며, 하루 최대 20회까지 발송 가능합니다.\n당신의 따뜻한 한마디가 누군가에게는 큰 힘이 됩니다. 😊`
    })
  }

  // 3. 날짜 구분선 및 실제 메시지 삽입 (시간순 정렬에 맞춤)
  let lastDate = null
  visibleMessages.forEach((msg) => {
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

const handleFileChange = async (e) => {
  const { files } = e.target
  if (!files) return
  const newFiles = Array.from(files)
  if (replyFiles.value.length + newFiles.length > 6) {
    await uiStore.showAlert('파일은 최대 6개까지만 첨부할 수 있습니다.', '알림')
    return
  }
  replyFiles.value = [...replyFiles.value, ...newFiles]
}

const removeFile = (idx) => { replyFiles.value.splice(idx, 1) }

const handleSendReply = async () => {
  if (isPartnerWithdrawn.value) {
    await uiStore.showAlert('탈퇴한 사용자에게는 메시지를 보낼 수 없습니다.', '안내')
    return
  }
  if (!replyContent.value.trim() && replyFiles.value.length === 0) return
  if (!selectedRoom.value || isRoomRestricted.value) return

  // [시니어 조치] 신뢰 점수 부족 시 메시지 발송 차단 (구체적인 점수 명시)
  const userKarma = authStore.user?.karmaPoint || 1000
  if (userKarma <= 800) {
    await uiStore.showAlert('⚠️ 신뢰 점수(Karma)가 80.0점 미만이라 쪽지 발송이 제한되었습니다.\n건전한 활동을 통해 80.0점 이상으로 회복해 주세요.', '발송 제한')
    return
  }
  
  const partnerId = getPartnerId(selectedRoom.value)
  isSendingReply.value = true

  let success = false
  if (editingMessage.value) {
    success = await messageStore.editMessage(editingMessage.value.id, replyContent.value)
    if (success) cancelEdit()
  } else {
    const result = await messageStore.sendMessage(partnerId, replyContent.value, replyFiles.value)
    success = result.success
    if (!success) await uiStore.showAlert(result.message, '오류')
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
  if (msg.isDeleted || isRoomRestricted.value) return
  editingMessage.value = msg
  replyContent.value = msg.content
}

const cancelEdit = () => {
  editingMessage.value = null
  replyContent.value = ''
}

const handleDeleteMessage = async (msgId) => {
  if (!await uiStore.showConfirm('이 메시지를 삭제하시겠습니까?', '메시지 삭제')) return
  const success = await messageStore.deleteMessage(msgId)
  if (success) {
    const partnerId = getPartnerId(selectedRoom.value)
    conversationList.value = await messageStore.fetchConversation(partnerId)
  }
}

const handleReportMsg = async (msg) => {
  const reason = prompt('이 메시지를 신고하는 사유를 입력해 주세요:', '부적절한 내용');
  if (reason === null) return;
  if (await uiStore.showConfirm('이 메시지를 신고하시겠습니까?', '메시지 신고')) {
    const result = await messageStore.reportMessage(msg.id, reason)
    if (result.success) await uiStore.showAlert('신고가 정상적으로 접수되었습니다.', '신고 완료')
    else await uiStore.showAlert(result.message, '오류')
  }
}

// [시니어 조치] 현재 대화방이 차단 상태인지 확인
const isRoomBlocked = computed(() => {
  if (!conversationList.value || conversationList.value.length === 0) return false
  // 대화 내역 중 가장 최신 메시지에서 차단 상태 확인
  const lastMsg = conversationList.value[conversationList.value.length - 1]
  return lastMsg.isBlockedByMe || lastMsg.isBlockedByPartner || lastMsg.isRoomRestricted
})

const blockedMessage = computed(() => {
  if (!conversationList.value || conversationList.value.length === 0) return ''
  const lastMsg = conversationList.value[conversationList.value.length - 1]
  if (lastMsg.isRoomRestricted) return '운영 정책에 의해 대화가 제한된 방입니다.'
  if (lastMsg.isBlockedByMe) return '차단한 사용자입니다. 메시지를 보낼 수 없습니다.'
  if (lastMsg.isBlockedByPartner) return '상대방에 의해 차단되어 메시지를 보낼 수 없습니다.'
  return ''
})

const handleUnblock = async (user) => {
  const userPublicId = user.publicId || user; // [시니어 조치] publicId 사용
  let msg = '차단을 해제하시겠습니까?';
  if (user.isSystemBlock) {
    msg = '⚠️ 잠깐! 이 사용자는 과거 심각한 운영 정책 위반으로 인해 시스템에서 자동차단한 대상입니다.\n\n차단을 해제하면 상대방이 다시 메시지를 보낼 수 있게 됩니다. 정말 해제하시겠습니까?';
  }

  if (await uiStore.showConfirm(msg, '차단 해제')) {
    const success = await messageStore.unblockUser(userPublicId)
    if (success) {
      blockedUsers.value = blockedUsers.value.filter(u => u.publicId !== userPublicId)
      await uiStore.showAlert('차단이 해제되었습니다.', '해제 완료')
    }
  }
}

const openBlockedList = async () => {
  blockedUsers.value = await messageStore.fetchBlockedUsers()
  showBlockedListModal.value = true
}

// [시니어 조치] 대화방 삭제 로직 (시스템 방 및 본인 방 완벽 제거)
const handleDeleteChat = async () => {
  if (!selectedRoom.value) return
  
  const partnerId = getPartnerId(selectedRoom.value)
  const isSystemRoom = !partnerId && selectedRoom.value.isSystem
  
  if (await uiStore.showConfirm('대화방을 나가시겠습니까? 삭제된 내역은 복구할 수 없습니다.', '대화방 나가기')) {
    // 1. UI에서 즉시 제거 (낙관적 업데이트)
    const currentPartnerId = partnerId
    messageStore.receivedMessages = messageStore.receivedMessages.filter(r => {
      const rpId = getPartnerId(r)
      if (currentPartnerId) return rpId !== currentPartnerId
      return rpId !== null // 시스템 방인 경우 모든 시스템 방 제거
    })

    const targetRoomId = selectedRoom.value.id
    selectedRoom.value = null
    conversationList.value = []
    currentMobileView.value = 'LIST'

    // 2. 서버 요청
    let success = false
    if (currentPartnerId) {
      success = await messageStore.deleteConversation(currentPartnerId)
    } else if (isSystemRoom) {
      success = await messageStore.deleteMessage(targetRoomId)
    }

    if (success) {
      await messageStore.fetchMessageRooms()
      await uiStore.showAlert('대화방이 삭제되었습니다.', '알림')
    } else {
      // 실패 시 목록 복구
      await messageStore.fetchMessageRooms()
    }
  }
}

// [시니어 조치] 사용자 차단 로직 (리스트에서 즉시 제거)
const handleBlock = async () => {
  const partnerId = getPartnerId(selectedRoom.value)
  if (!partnerId) return
  
  if (await uiStore.showConfirm('이 사용자를 차단하시겠습니까? 차단하면 서로 메시지를 주고받을 수 없으며 대화방이 목록에서 즉시 사라집니다.', '사용자 차단')) {
    // 1. UI에서 즉시 제거
    const currentPartnerId = partnerId
    messageStore.receivedMessages = messageStore.receivedMessages.filter(r => getPartnerId(r) !== currentPartnerId)
    
    selectedRoom.value = null
    conversationList.value = []
    currentMobileView.value = 'LIST'

    // 2. 서버 요청
    const blockSuccess = await messageStore.blockUser(currentPartnerId)
    if (blockSuccess) {
      await messageStore.fetchMessageRooms()
      await uiStore.showAlert('차단되었습니다.', '차단 완료')
    } else {
      await messageStore.fetchMessageRooms()
    }
  }
}

// [시니어 조치] 시스템 메시지 유형별 클래스 반환
const getSystemMessageClass = (content) => {
  if (!content) return ''
  if (content.includes('[주의]')) return 'is-caution'
  if (content.includes('[안내]')) return 'is-info'
  if (content.includes('[경고]')) return 'is-warning'
  return ''
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
        <div class="sidebar-top-row">
          <div class="sidebar-title-row">
            <h2 class="sidebar-title">Direct Message</h2>
            <div class="karma-badge-mini">
              <span class="karma-value">{{ displayKarma }} P</span>
            </div>
          </div>
          <button class="manage-block-btn" @click="openBlockedList" title="차단 목록 관리">🚫</button>
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
          :class="{ 'active': selectedRoom && (selectedRoom.id === room.id) }"
          @click="selectRoom(room)"
        >
          <div class="item-avatar">
            <span v-if="room.isSystem && !room.sender && !room.receiverId" class="system-icon">📢</span>
            <div v-else class="user-avatar-wrap">
              <div class="user-avatar-small">
                {{ (isMe(room.sender?.publicId) ? room.receiverNickname : room.sender?.nickname)?.[0] }}
              </div>
              <div v-if="!room.isSystem" class="online-indicator-dot" :class="{ 'is-online': room.isPartnerOnline }"></div>
            </div>
          </div>
          <div class="item-main">
            <div class="item-top">
              <div class="item-name-group">
                <span class="item-name">
                  {{ (room.isSystem && !room.sender && !room.receiverId) ? '시스템' : (isMe(room.sender?.publicId) ? room.receiverNickname : room.sender?.nickname) }}
                </span>
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
              {{ (selectedRoom.isSystem && !selectedRoom.sender ? 'S' : (isMe(selectedRoom.sender?.publicId) ? selectedRoom.receiverNickname : selectedRoom.sender?.nickname)?.[0]) }}
            </div>
            <div class="name-box">
              <span class="partner-name">{{ selectedRoom.isSystem && !selectedRoom.sender ? '시스템 메시지' : (isMe(selectedRoom.sender?.publicId) ? selectedRoom.receiverNickname : selectedRoom.sender?.nickname) }}</span>
              <span class="partner-status" :class="{ 'is-offline': !selectedRoom.isPartnerOnline }" v-if="!selectedRoom.isSystem || selectedRoom.sender">
                {{ selectedRoom.isPartnerOnline ? '활동 중' : '오프라인' }}
              </span>
            </div>
          </div>
          <div class="header-actions">
            <div class="menu-container" v-click-outside="() => showMenu = false">
              <button class="icon-btn" @click="showMenu = !showMenu">ⓘ</button>
              <div v-if="showMenu" class="dropdown-menu">
                <button @click="handleDeleteChat" class="menu-item delete">대화방 삭제</button>
                <button v-if="getPartnerId(selectedRoom)" @click="handleBlock" class="menu-item block">사용자 차단</button>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-area thin-scrollbar" ref="chatScrollRef">
          <div v-for="msg in processedConversation" :key="msg.id" class="msg-row-container" :id="'msg-' + msg.id">
            <div v-if="msg.isDateHeader" class="date-header"><span>{{ msg.content }}</span></div>
            
            <!-- 시스템 가이드 (클릭 시 이동 로직 포함) -->
            <div v-else-if="msg.isSystem" class="system-guide-row">
              <div class="system-guide-bubble" 
                   :class="[getSystemMessageClass(msg.content), { 'clickable': msg.relatedTargetId }]"
                   @click="scrollToMessage(msg.relatedTargetId)">
                <p class="pre-wrap">{{ msg.content }}</p>
                <small v-if="msg.relatedTargetId" class="click-info-text">클릭하여 해당 메시지 보기</small>
              </div>
            </div>

            <div v-else-if="msg.isDeleted && !msg.isReported" class="deleted-msg-row">
              <div class="deleted-bubble">메시지가 삭제 되었습니다.</div>
            </div>

            <div v-else class="msg-row" :class="{ 'msg-me': isMe(msg.sender?.publicId) }">
              <div class="msg-bubble-wrap">
                <div class="msg-bubble-group">
                  <div v-if="isMe(msg.sender?.publicId)" class="msg-side-meta">
                    <span class="msg-time-inline">{{ formatTimeOnly(msg.createdAt) }}</span>
                    <!-- 제한된 방이 아닐 때만 수정/삭제 노출 -->
                    <div v-if="!isRoomRestricted" class="msg-side-actions">
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
                    <!-- 제한된 방이 아닐 때만 신고 노출 -->
                    <div v-if="!isRoomRestricted" class="msg-side-actions">
                      <button class="msg-action-btn report" @click="handleReportMsg(msg)" title="신고">🚩</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 하단 입력 영역 (1:1 대화방인 경우 항상 표시, 영구 제한/차단 시 비활성화) -->
        <div class="input-container" v-if="getPartnerId(selectedRoom) || isPartnerWithdrawn">
          <!-- [시니어 조치] 차단/제한 상태 또는 상대방 탈퇴 시 안내 문구 표시 -->
          <div v-if="isRoomBlocked || isPartnerWithdrawn" class="blocked-notice-area">
            <div class="blocked-notice-box">
              <span class="b-icon">{{ isPartnerWithdrawn ? '👤' : '🔒' }}</span>
              <p class="b-text">{{ isPartnerWithdrawn ? '상대방이 서비스를 탈퇴하여 더 이상 메시지를 보낼 수 없습니다.' : blockedMessage }}</p>
            </div>
          </div>

          <template v-else>
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
                <textarea 
                  v-model="replyContent" 
                  :placeholder="editingMessage ? '메시지 수정...' : '메시지 보내기...'" 
                  @keydown.enter.exact.prevent="handleSendReply" 
                  :disabled="isSendingReply" 
                  rows="3"
                ></textarea>
              </div>
              <div class="input-footer-actions">
                <button class="real-send-btn" @click="handleSendReply" :disabled="isSendingReply || (!replyContent.trim() && replyFiles.length === 0)" :class="{ 'active': (replyContent.trim() || replyFiles.length > 0) }">
                  {{ editingMessage ? '수정 완료' : '전송' }}
                </button>
              </div>
            </div>
          </template>
        </div>
      </template>
      <div v-else class="main-empty">
        <div class="dm-large-icon">✉️</div>
        <h2>내 메시지</h2>
        <p>친구나 커뮤니티 멤버에게 메시지를 보내보세요.</p>
        <button class="primary-btn" @click="messageStore.fetchMessageRooms()">새 메시지 확인</button>
      </div>
    </div>

    <!-- 차단 목록 관리 모달 -->
    <Teleport to="body">
      <div v-if="showBlockedListModal" class="block-modal-overlay" @click.self="showBlockedListModal = false">
        <div class="block-modal-content">
          <div class="block-modal-header">
            <h3>🚫 차단된 사용자 관리</h3>
            <button class="close-btn" @click="showBlockedListModal = false">&times;</button>
          </div>
          <div class="blocked-list">
            <div v-if="blockedUsers.length === 0" class="empty-block-msg">차단된 사용자가 없습니다.</div>
            <div v-for="u in blockedUsers" :key="u.id" class="blocked-item">
              <div class="blocked-info">
                <div class="blocked-nickname-row">
                  <span class="blocked-nickname">{{ u.nickname }}</span>
                  <span v-if="u.isSystemBlock" class="system-block-badge">⚠️ 운영원칙 위반</span>
                </div>
                <div v-if="u.reason" class="blocked-reason">{{ u.reason }}</div>
              </div>
              <button class="unblock-btn" :class="{ 'is-system': u.isSystemBlock }" @click="handleUnblock(u)">차단 해제</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.dm-container { display: flex; height: 750px; background: var(--card-bg); border-radius: 16px; border: 1px solid var(--border-color); overflow: hidden; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1); }
.thin-scrollbar::-webkit-scrollbar { width: 4px; height: 4px; }
.thin-scrollbar::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 10px; }

/* 사이드바 */
.dm-sidebar { width: 350px; flex-shrink: 0; border-right: 1px solid var(--border-color); display: flex; flex-direction: column; background: var(--bg-primary); }
.sidebar-header { padding: 24px 20px; border-bottom: 1px solid var(--border-color); }
.sidebar-top-row { display: flex; justify-content: space-between; align-items: center; }
.sidebar-title-row { display: flex; align-items: center; gap: 10px; }
.sidebar-title { font-size: 1.2rem; font-weight: 950; margin: 0; letter-spacing: -0.8px; white-space: nowrap; }
.karma-badge-mini { background: rgba(168, 85, 247, 0.1); padding: 2px 8px; border-radius: 12px; border: 1px solid rgba(168, 85, 247, 0.2); }
.karma-value { font-size: 0.75rem; font-weight: 900; color: #a855f7; }
.manage-block-btn { background: none; border: none; font-size: 1.2rem; cursor: pointer; opacity: 0.6; transition: 0.2s; padding: 5px; }
.manage-block-btn:hover { opacity: 1; transform: scale(1.1); }

.dm-list { flex: 1; overflow-y: auto; }
.dm-item { padding: 16px 20px; display: flex; align-items: center; gap: 14px; cursor: pointer; transition: 0.2s; position: relative; }
.dm-item:hover { background: var(--hover-bg); }
.dm-item.active { background: var(--hover-bg); }
.dm-item.active::after { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: #a855f7; }

.user-avatar-wrap { position: relative; }
.user-avatar-small { width: 52px; height: 52px; border-radius: 50%; background: linear-gradient(135deg, #c084fc, #6366f1); color: white; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 1.3rem; }
.online-indicator-dot { position: absolute; bottom: 2px; right: 2px; width: 14px; height: 14px; border-radius: 50%; background: #94a3b8; border: 2px solid var(--bg-primary); }
.online-indicator-dot.is-online { background: #10b981; }

.item-main { flex: 1; min-width: 0; }
.item-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.item-name-group { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
.item-name { font-weight: 800; font-size: 0.95rem; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.unread-badge-circle { background: #ef4444; color: white; font-size: 0.65rem; font-weight: 900; width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; line-height: 1; }
.item-date { font-size: 0.75rem; color: var(--text-muted); flex-shrink: 0; margin-left: 10px; }
.item-preview { font-size: 0.85rem; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0; }
.unread-text { color: var(--text-primary); font-weight: 700; }

.dm-main { flex: 1; min-width: 0; display: flex; flex-direction: column; background: var(--card-bg); position: relative; }
.main-header { padding: 15px 20px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; background: var(--card-bg); backdrop-filter: blur(15px); z-index: 50; }
.partner-meta { display: flex; align-items: center; gap: 12px; }
.mobile-back-btn { display: none; }
.avatar-circle { width: 40px; height: 40px; border-radius: 50%; background: var(--hover-bg); display: flex; align-items: center; justify-content: center; font-weight: 900; color: #a855f7; border: 1px solid var(--border-color); }
.partner-name { font-weight: 850; font-size: 1.1rem; color: var(--text-primary); }
.partner-status { font-size: 0.75rem; color: #10b981; font-weight: 700; margin-top: 2px; display: block; }
.partner-status.is-offline { color: var(--text-muted); }

.icon-btn { background: none; border: none; font-size: 1.4rem; cursor: pointer; color: var(--text-muted); transition: 0.2s; padding: 5px; }
.dropdown-menu { position: absolute; right: 0; top: 100%; width: 170px; background: var(--card-bg); border: 1.5px solid var(--border-color); border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.15); z-index: 100; overflow: hidden; }
.menu-item { width: 100%; padding: 12px 16px; border: none; background: none; text-align: left; font-size: 0.9rem; font-weight: 700; cursor: pointer; color: var(--text-primary); transition: 0.2s; }
.menu-item:hover { background: var(--hover-bg); }
.menu-item.delete { border-bottom: 1px solid var(--border-color); }
.menu-item.block { color: #e67e22; }

.chat-area { flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 15px; }
.date-header { display: flex; justify-content: center; margin: 25px 0; position: relative; }
.date-header span { background: var(--card-bg); padding: 5px 15px; border-radius: 20px; font-size: 0.7rem; color: var(--text-muted); font-weight: 800; border: 1px solid var(--border-color); z-index: 1; }

.system-guide-bubble { max-width: 85%; background: var(--bg-primary); border: 1px dashed var(--border-color); padding: 15px; border-radius: 15px; margin: 0 auto 20px; transition: all 0.3s; position: relative; }
.system-guide-bubble.clickable { cursor: pointer; border-style: solid; border-color: #a855f7; }
.system-guide-bubble.clickable:hover { background: var(--hover-bg); transform: translateY(-2px); }
.system-guide-bubble p { font-size: 0.8rem; color: var(--text-secondary); line-height: 1.6; margin: 0; text-align: center; }
.pre-wrap { white-space: pre-wrap !important; word-break: break-all; }

/* [시니어 조치] 시스템 메시지 유형별 색상 적용 */
.system-guide-bubble.is-caution { border-color: #ef4444; background: rgba(239, 68, 68, 0.05); }
.system-guide-bubble.is-caution p { color: #ef4444; font-weight: 700; }
.system-guide-bubble.is-caution .click-info-text { color: #ef4444; }

.system-guide-bubble.is-info { border-color: #a855f7; background: rgba(168, 85, 247, 0.05); }
.system-guide-bubble.is-info p { color: #a855f7; font-weight: 700; }
.system-guide-bubble.is-info .click-info-text { color: #a855f7; }

.system-guide-bubble.is-warning { border-color: #f59e0b; background: rgba(245, 158, 11, 0.05); }
.system-guide-bubble.is-warning p { color: #f59e0b; font-weight: 700; }
.system-guide-bubble.is-warning .click-info-text { color: #f59e0b; }

.click-info-text { display: block; font-size: 0.65rem; color: #a855f7; margin-top: 8px; font-weight: 700; opacity: 0.8; }

.deleted-msg-row { display: flex; justify-content: center; margin: 15px 0; width: 100%; }
.deleted-bubble { background: var(--bg-primary); color: var(--text-muted); font-size: 0.8rem; padding: 10px 30px; border-radius: 25px; border: 1.5px dashed var(--border-color); font-weight: 600; font-style: italic; }

/* 하이라이트 애니메이션 (말풍선 중심) */
@keyframes bubble-flash { 
  0% { box-shadow: 0 0 0 0 rgba(168, 85, 247, 0); }
  30% { box-shadow: 0 0 0 10px rgba(168, 85, 247, 0.3); background-color: rgba(168, 85, 247, 0.1); }
  100% { box-shadow: 0 0 0 0 rgba(168, 85, 247, 0); }
}
.highlight-flash { animation: bubble-flash 2s ease-out; }

.msg-row { display: flex; width: 100%; }
.msg-me { justify-content: flex-end; }
.msg-bubble-wrap { max-width: 75%; display: flex; flex-direction: column; gap: 4px; }
.msg-bubble-group { display: flex; align-items: flex-end; gap: 12px; }

.msg-bubble { padding: 12px 16px; border-radius: 20px; position: relative; box-shadow: 0 2px 8px rgba(0,0,0,0.03); min-width: 40px; }
.msg-me .msg-bubble { background: linear-gradient(135deg, #a855f7, #6366f1); color: white; border-bottom-right-radius: 4px; }
.msg-row:not(.msg-me) .msg-bubble { background: var(--hover-bg); color: var(--text-primary); border: 1px solid var(--border-color); border-bottom-left-radius: 4px; }

.edited-tag { font-size: 0.6rem; opacity: 0.7; margin-left: 4px; font-weight: 400; }

.msg-side-meta { display: flex; flex-direction: column; justify-content: flex-end; min-width: 50px; height: 100%; padding-bottom: 4px; }
.msg-me .msg-side-meta { align-items: flex-end; }
.msg-time-inline { font-size: 0.6rem; color: var(--text-muted); font-weight: 600; }
.msg-side-actions { display: none; gap: 6px; }
.msg-action-btn { background: var(--bg-primary); border: 1px solid var(--border-color); width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.7rem; cursor: pointer; transition: 0.2s; }
.msg-action-btn:hover { background: var(--hover-bg); transform: scale(1.1); color: #a855f7; }
.msg-action-btn.report:hover { color: #ef4444; }
.msg-row:hover .msg-time-inline { display: none; }
.msg-row:hover .msg-side-actions { display: flex; }

.edit-preview-bar { background: var(--bg-primary); border: 1.5px solid #a855f7; border-bottom: none; padding: 10px 15px; display: flex; justify-content: space-between; align-items: center; border-radius: 20px 20px 0 0; }
.edit-info { flex: 1; min-width: 0; }
.edit-label { font-size: 0.65rem; font-weight: 800; color: #a855f7; display: block; margin-bottom: 2px; }
.edit-text-preview { font-size: 0.8rem; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0; }
.cancel-edit-btn { background: none; border: none; font-size: 1rem; color: var(--text-muted); cursor: pointer; padding: 5px; display: flex; align-items: center; justify-content: center; }
.cancel-edit-btn:hover { color: #ef4444; }

/* 파일 첨부 미리보기 스타일 */
.f-previews { 
  display: flex; 
  flex-wrap: wrap; /* PC에서 2행 이상으로 배치되도록 설정 */
  gap: 8px; 
  padding: 10px 15px; 
  background: var(--bg-primary); 
  border: 1px solid var(--border-color); 
  border-bottom: none; 
  border-radius: 12px 12px 0 0; 
  max-height: 120px; /* 너무 커지지 않도록 최대 높이 설정 */
  overflow-y: auto; /* 내용이 많으면 세로 스크롤 발생 */
  overflow-x: hidden; 
}

/* 스크롤바 가시화 (가려져 있던 스크롤바를 보이게 설정) */
.f-previews::-webkit-scrollbar { 
  width: 6px; 
  height: 6px; 
  display: block; /* 가려져 있던 스크롤바를 강제로 노출 */
}
.f-previews::-webkit-scrollbar-track { background: var(--hover-bg); border-radius: 10px; }
.f-previews::-webkit-scrollbar-thumb { background: rgba(168, 85, 247, 0.3); border-radius: 10px; }
.f-previews::-webkit-scrollbar-thumb:hover { background: rgba(168, 85, 247, 0.5); }

.f-chip { display: inline-flex; align-items: center; gap: 6px; background: var(--hover-bg); border: 1px solid var(--border-color); padding: 4px 10px; border-radius: 20px; flex-shrink: 0; }
.f-name { font-size: 0.7rem; font-weight: 700; color: var(--text-primary); max-width: 120px; overflow: hidden; text-overflow: ellipsis; }
.f-remove { background: none; border: none; font-size: 1.1rem; color: var(--text-muted); cursor: pointer; padding: 0; line-height: 1; display: flex; align-items: center; }
.f-remove:hover { color: #ef4444; }

.input-container { padding: 15px 20px; border-top: 1px solid var(--border-color); }
.input-info-bar { display: flex; justify-content: space-between; align-items: center; font-size: 0.75rem; color: var(--text-secondary); margin-bottom: 12px; padding: 0 5px; }
.limit-text strong { color: #a855f7; font-weight: 900; }
.cost-text { background: rgba(168, 85, 247, 0.1); color: #a855f7; padding: 3px 10px; border-radius: 6px; font-weight: 800; font-size: 0.7rem; }

.input-form-wrapper { background: var(--hover-bg); border-radius: 20px; border: 1px solid var(--border-color); padding: 10px 16px; display: flex; flex-direction: column; transition: all 0.3s; }
.input-form-wrapper.editing-active { border-radius: 0 0 20px 20px; border-top: none; background: var(--card-bg); border-color: #a855f7; }
.input-form-wrapper.is-restricted { background: #f8f9fa; opacity: 0.8; border-style: dashed; }
.input-form-wrapper:focus-within { border-color: #a855f7; background: var(--card-bg); }

.input-field-container { display: flex; gap: 12px; align-items: flex-start; width: 100%; }
.attach-trigger { font-size: 1.4rem; cursor: pointer; padding-top: 8px; filter: grayscale(1); opacity: 0.6; }
textarea { flex: 1; width: 100%; border: none; background: transparent; padding: 8px 0; resize: none; font-size: 0.95rem; color: var(--text-primary); outline: none; min-height: 45px; }
textarea:disabled { cursor: not-allowed; color: var(--text-muted); }
textarea::-webkit-scrollbar { width: 3px; }
textarea::-webkit-scrollbar-thumb { background: rgba(168, 85, 247, 0.2); border-radius: 10px; }

.input-footer-actions { display: flex; justify-content: flex-end; width: 100%; margin-top: 4px; }

/* [시니어 조치] 차단 안내 UI 스타일 */
.blocked-notice-area {
  padding: 15px 20px;
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
}
.blocked-notice-box {
  background: var(--hover-bg);
  border: 1px dashed var(--border-color);
  border-radius: 16px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-secondary);
}
.b-icon { font-size: 1.2rem; }
.b-text { margin: 0; font-size: 0.85rem; font-weight: 700; line-height: 1.4; }

.real-send-btn { background: none; border: none; color: #a855f7; font-weight: 950; cursor: pointer; opacity: 0.3; transition: 0.2s; padding: 5px 10px; }
.real-send-btn.active { opacity: 1; transform: scale(1.05); }
.real-send-btn:disabled { cursor: not-allowed; opacity: 0.2; }

.dm-img-thumb { max-width: 200px; max-height: 200px; border-radius: 12px; margin-top: 8px; cursor: pointer; object-fit: cover; border: 1px solid var(--border-color); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.empty-list-sidebar { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; text-align: center; color: var(--text-muted); }
.empty-icon-mini { font-size: 2.5rem; margin-bottom: 15px; opacity: 0.6; }
.empty-list-sidebar p { font-size: 0.9rem; font-weight: 600; margin: 0; }

.main-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 40px; background: var(--card-bg); }
.dm-large-icon { font-size: 5rem; margin-bottom: 25px; background: var(--hover-bg); width: 120px; height: 120px; display: flex; align-items: center; justify-content: center; border-radius: 50%; border: 1px solid var(--border-color); }
.main-empty h2 { font-size: 1.6rem; font-weight: 900; margin: 0 0 12px; color: var(--text-primary); }
.main-empty p { font-size: 1rem; color: var(--text-secondary); margin: 0 0 30px; font-weight: 500; }
.primary-btn { background: linear-gradient(135deg, #a855f7, #6366f1); color: white; border: none; padding: 12px 28px; border-radius: 10px; font-weight: 800; font-size: 0.95rem; cursor: pointer; transition: 0.3s; box-shadow: 0 4px 15px rgba(168, 85, 247, 0.25); }

/* 차단 관리 모달 스타일 */
.block-modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 10000; backdrop-filter: blur(4px); }
.block-modal-content { background: var(--card-bg); width: 90%; max-width: 420px; border-radius: 20px; padding: 25px; border: 1.5px solid var(--border-color); box-shadow: 0 20px 50px rgba(0,0,0,0.3); }
.block-modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.block-modal-header h3 { margin: 0; font-size: 1.1rem; font-weight: 900; }
.close-btn { background: none; border: none; font-size: 1.8rem; color: var(--text-muted); cursor: pointer; line-height: 1; transition: color 0.2s; padding: 0; display: flex; align-items: center; justify-content: center; }
.close-btn:hover { color: var(--text-primary); }
.blocked-list { max-height: 350px; overflow-y: auto; padding-right: 5px; }
.empty-block-msg { text-align: center; padding: 30px; color: var(--text-muted); font-size: 0.9rem; }

.blocked-item { display: flex; justify-content: space-between; align-items: center; padding: 15px; background: var(--bg-primary); border-radius: 12px; margin-bottom: 10px; border: 1px solid var(--border-color); transition: all 0.2s; }
.blocked-item:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

.blocked-info { flex: 1; display: flex; flex-direction: column; gap: 4px; padding-right: 10px; }
.blocked-nickname-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.blocked-nickname { font-weight: 800; color: var(--text-primary); font-size: 0.95rem; }
.system-block-badge { background: #fff0f0; color: #ff4d4d; font-size: 10px; font-weight: 900; padding: 2px 6px; border-radius: 4px; border: 1px solid rgba(255, 77, 77, 0.2); white-space: nowrap; }
.blocked-reason { font-size: 11px; color: var(--text-muted); font-style: italic; line-height: 1.3; }

.unblock-btn { background: var(--hover-bg); color: var(--text-secondary); border: 1px solid var(--border-color); padding: 8px 12px; border-radius: 8px; font-size: 0.75rem; font-weight: 800; cursor: pointer; transition: 0.2s; white-space: nowrap; }
.unblock-btn:hover { background: var(--divider-color); color: var(--text-primary); }
.unblock-btn.is-system { background: #fff0f0; color: #ff4d4d; border-color: rgba(255, 77, 77, 0.1); }
.unblock-btn.is-system:hover { background: #ff4d4d; color: white; }

@media (max-width: 768px) {
  .dm-container { height: 85vh; border-radius: 0; border: none; }
  .dm-sidebar { width: 100%; border-right: none; }
  .dm-sidebar.mobile-hide { display: none; }
  .dm-main { display: none; width: 100%; }
  .dm-main.mobile-show { display: flex; }
  .mobile-back-btn { display: block; background: none; border: none; font-size: 1.5rem; margin-right: 10px; color: var(--text-primary); }
  .dm-item { padding: 10px 12px; gap: 10px; }
  .user-avatar-small { width: 38px; height: 38px; font-size: 1rem; }
  .msg-text { font-size: 0.8rem; }
  .dm-img-thumb { max-width: 100px; max-height: 100px; }
}
</style>