<template>
  <div class="barter-tab-container">
    <div class="barter-summary-cards">
      <div class="summary-card" @click="filterStatus = 'ALL'" :class="{ active: filterStatus === 'ALL' }">
        <span class="s-label">전체 내역</span>
        <span class="s-value">{{ proposals.length }}</span>
      </div>
      <div class="summary-card" @click="filterStatus = 'PROPOSED'" :class="{ active: filterStatus === 'PROPOSED' }">
        <span class="s-label">받은 제안</span>
        <span class="s-value">{{ receivedProposalsCount }}</span>
      </div>
      <div class="summary-card" @click="filterStatus = 'NEGOTIATING'" :class="{ active: filterStatus === 'NEGOTIATING' }">
        <span class="s-label">협의 중</span>
        <span class="s-value">{{ negotiatingCount }}</span>
      </div>
    </div>

    <div v-if="loading" class="loading-state">데이터를 불러오는 중...</div>
    <div v-else-if="filteredProposals.length > 0" class="proposals-list">
      <div v-for="proposal in filteredProposals" :key="proposal.id" class="proposal-item" :data-proposal-id="proposal.id">
        <!-- 아코디언 헤더 (항상 표시) -->
        <div class="proposal-header-accordion" @click="toggleExpanded(proposal.id)">
          <div class="header-left">
            <span class="accordion-toggle" :class="{ expanded: isExpanded(proposal.id) }">
              {{ isExpanded(proposal.id) ? '▼' : '▶' }}
            </span>
            <span class="status-chip" :style="{ backgroundColor: getStatusConfig(proposal.status).color }">
              {{ getStatusConfig(proposal.status).label }}
            </span>
            <span v-if="proposal.proposerPublicId === authStore.user.publicId" class="my-proposal-chip">
              내가 보낸 제안
            </span>
          </div>
          <span class="proposal-date">{{ formatDate(proposal.createdAt) }}</span>
        </div>

        <!-- 아코디언 바디 (펼쳤을 때만 표시) -->
        <div v-if="isExpanded(proposal.id)" class="proposal-body">
        <div class="trade-info-main">
          <!-- 나의 물건 (제안자: offeredPost, 수신자: barterPost) -->
          <div class="item-box offer">
            <span class="box-label">나의 상품</span>
            <div class="item-content" @click="viewPost(getMyItem(proposal).id, proposal.id)">
              <div class="p-text-group">
                <p class="p-title">{{ getMyItem(proposal).itemName || getMyItem(proposal).title }}</p>
                <p v-if="getMyItem(proposal).itemName" class="p-item-name">{{ getMyItem(proposal).title }}</p>
              </div>
              <img v-if="getMyItem(proposal).imageUrls?.length" :src="getMyItem(proposal).imageUrls[0]" class="mini-thumb-right" />
              <div v-else class="mini-thumb-placeholder">📦</div>
            </div>
          </div>
          <div class="trade-arrow pc-icon">⇄</div>
          <div class="trade-arrow mobile-icon">⇅</div>
          <!-- 상대 물건 (제안자: barterPost, 수신자: offeredPost) -->
          <div class="item-box target">
            <span class="box-label">상대 상품</span>
            <div class="item-content" @click="viewPost(getOpponentItem(proposal).id, proposal.id)">
              <div class="p-text-group">
                <p class="p-title">{{ getOpponentItem(proposal).itemName || getOpponentItem(proposal).title }}</p>
                <p v-if="getOpponentItem(proposal).itemName" class="p-item-name">{{ getOpponentItem(proposal).title }}</p>
              </div>
              <img v-if="getOpponentItem(proposal).imageUrls?.length" :src="getOpponentItem(proposal).imageUrls[0]" class="mini-thumb-right" />
              <div v-else class="mini-thumb-placeholder">📦</div>
            </div>
          </div>
        </div>

        <div class="proposal-message" v-if="proposal.message">
          <span class="m-icon">💬</span>
          <p class="m-text">{{ proposal.message }}</p>
        </div>

        <div class="proposal-actions">
          <button class="btn-message" @click.stop="goToMessage(proposal)">✉️쪽지보기</button>

          <!-- Phase 1: PROPOSED 상태 (수신자에게만 표시) -->
          <template v-if="proposal.status === 'PROPOSED' && proposal.receiverPublicId === authStore.user.publicId">
            <button class="btn-action accept" @click.stop="respond(proposal.id, 'ACCEPTED')">✅ 제안수락</button>
            <button class="btn-action reject" @click.stop="respond(proposal.id, 'REJECTED')">❌ 제안거절</button>
          </template>

          <!-- 상세보기 버튼 (PROPOSED 제외, 제안 수락 후부터 표시) -->
          <button v-if="proposal.status !== 'PROPOSED'" class="btn-detail-view" @click.stop="openDetailModal(proposal)">
            📋 상세보기
          </button>
        </div>
        </div>
        <!-- proposal-body 닫음 -->
      </div>
      <!-- proposal-item 닫음 -->
    </div>
    <div v-else class="empty-state">
      <span class="empty-icon">🔄</span>
      <p>물물교환 내역이 없습니다.</p>
    </div>

    <!-- 거래 제안 상세 모달 -->
    <BarterProposalDetailModal
      v-if="isDetailModalOpen"
      :proposal="selectedDetailProposal"
      @close="isDetailModalOpen = false"
      @success="handleDetailModalSuccess"
    />

    <!-- 질문 모달 -->
    <GlobalModal v-if="isQuestionModalOpen" @close="isQuestionModalOpen = false">
      <template #header><h3>질문 보내기</h3></template>
      <template #body>
        <div class="question-modal-body">
          <p class="q-guide">상대방에게 물건 상태나 거래 방식에 대해 질문해 보세요.</p>
          <textarea v-model="questionContent" class="q-textarea" maxlength="50" placeholder="질문을 입력하세요 (최대 50자)"></textarea>
          <p class="q-notice">💡 질문 시 카르마 0.1P가 소모됩니다. ({{ selectedProposal?.questionCount }}/3회)</p>
        </div>
      </template>
      <template #footer>
        <div class="modal-footer-btns">
          <button class="btn-modal-cancel" @click="isQuestionModalOpen = false">취소</button>
          <button class="btn-modal-submit" @click="sendQuestion" :disabled="!questionContent.trim()">질문 전송</button>
        </div>
      </template>
    </GlobalModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { useNotificationStore } from '@/stores/notification'
import axios from '@/plugins/axios'
import { PROPOSAL_STATUS } from '@/constants/postConstants'
import BarterProposalDetailModal from '@/components/BarterProposalDetailModal.vue'
import GlobalModal from '@/components/common/GlobalModal.vue'

const authStore = useAuthStore()
const uiStore = useUiStore()
const notificationStore = useNotificationStore()
const router = useRouter()
const route = useRoute()

const proposals = ref([])
const loading = ref(true)
const filterStatus = ref('ALL')
const expandedProposalIds = ref(new Set())

const isQuestionModalOpen = ref(false)
const isDetailModalOpen = ref(false)
const selectedDetailProposal = ref(null)
const questionContent = ref('')

const receivedProposalsCount = computed(() => proposals.value.filter(p => p.receiverId === authStore.user.id && p.status === 'PROPOSED').length)
const negotiatingCount = computed(() => proposals.value.filter(p => p.status === 'NEGOTIATING' || p.status === 'ACCEPTED').length)

const filteredProposals = computed(() => {
  if (filterStatus.value === 'ALL') return proposals.value
  if (filterStatus.value === 'PROPOSED') return proposals.value.filter(p => p.receiverId === authStore.user.id && p.status === 'PROPOSED')
  return proposals.value.filter(p => p.status === filterStatus.value)
})

const getStatusConfig = (status) => PROPOSAL_STATUS[status] || { label: status, color: '#6b7280' }

const getOpponentItem = (proposal) => {
  return proposal.receiverPublicId === authStore.user.publicId ? proposal.offeredPost : proposal.barterPost
}

const getMyItem = (proposal) => {
  return proposal.receiverPublicId === authStore.user.publicId ? proposal.barterPost : proposal.offeredPost
}

const fetchProposals = async () => {
  try {
    const res = await axios.get('/api/barter/proposals')
    proposals.value = res.data
  } catch (e) {
    console.error('제안 목록 로드 실패:', e)
  } finally {
    loading.value = false
  }
}

const respond = async (id, status) => {
  try {
    await axios.post(`/api/barter/proposals/${id}/respond`, null, { params: { status } })
    await uiStore.showAlert(`${status === 'ACCEPTED' ? '수락' : '거절'} 처리되었습니다.`, '완료')
    await fetchProposals()
  } catch (e) {
    await uiStore.showAlert('처리에 실패했습니다.', '오류')
  }
}

const acceptSchedule = async (id) => {
  try {
    await axios.post(`/api/barter/proposals/${id}/schedule/accept`)
    await uiStore.showAlert('거래 조건을 수락했습니다!', '완료')
    fetchProposals()
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '수락 처리에 실패했습니다.', '오류')
  }
}

const completeTrade = async (id) => {
  const proposal = proposals.value.find(p => p.id === id)
  if (!proposal) return

  // 이미 완료한 경우
  if (isMyTradeCompleted(proposal)) {
    await uiStore.showAlert('⏳ 상대방이 거래 완료를 표시했습니다. 거래 확인 후 거래 완료 표시를 진행해주세요.', '거래 대기 중')
    return
  }

  const isConfirmed = await uiStore.showConfirm(
    '물건을 실제로 교환하셨나요?\n\n거래를 완료하면:\n• 경험치 100EXP 획득',
    '거래 완료',
    '완료',
    '취소'
  )
  if (!isConfirmed) return

  try {
    const response = await axios.post(`/api/barter/proposals/${id}/complete`)

    // ✅ proposals 배열에서 해당 proposal을 찾아서 즉시 업데이트
    const index = proposals.value.findIndex(p => p.id === id)
    if (index !== -1) {
      proposals.value[index] = response.data
    }

    if (isTradeCompleted(response.data)) {
      await uiStore.showAlert('양쪽 모두 거래를 완료했습니다!\n경험치 100EXP를 획득했습니다.', '거래 완료')
    } else {
      await uiStore.showAlert('당신의 거래 완료가 확인되었습니다.\n상대방의 확인을 기다리는 중입니다.', '완료 대기 중')
    }

    // ✅ 동기화 목적으로 나머지 데이터는 다시 가져오기 (await로 기다림)
    await fetchProposals()
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '거래 완료 처리에 실패했습니다.', '오류')
  }
}

const cancelTrade = async (id) => {
  const isConfirmed = await uiStore.showConfirm(
    '거래를 취소하면 물건 상태가 교환 가능 상태로 돌아갑니다.\n정말 취소하시겠습니까?',
    '거래 취소',
    '취소하기',
    '뒤로'
  )
  if (!isConfirmed) return

  try {
    await axios.post(`/api/barter/proposals/${id}/cancel`)
    await uiStore.showAlert('거래가 취소되었습니다.', '완료')
    fetchProposals()
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '취소 처리에 실패했습니다.', '오류')
  }
}

const openQuestionModal = async (proposal) => {
  if (proposal.questionCount >= 3) {
    await uiStore.showAlert('질문은 최대 3회까지만 가능합니다.', '안내')
    return
  }
  selectedProposal.value = proposal
  questionContent.value = ''
  isQuestionModalOpen.value = true
}

const sendQuestion = async () => {
  try {
    await axios.post(`/api/barter/proposals/${selectedProposal.value.id}/questions`, null, {
      params: { content: questionContent.value }
    })
    await uiStore.showAlert('질문을 보냈습니다. 카르마 0.1P가 소모되었습니다.', '전송 완료')
    isQuestionModalOpen.value = false
    fetchProposals()
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '질문 전송에 실패했습니다.', '오류')
  }
}

// [거래 조건 상태 판단]
const getScheduleStatus = (proposal) => {
  if (!proposal.proposerScheduleJson && !proposal.receiverScheduleJson) {
    return 'NO_SCHEDULE'
  }
  if (proposal.agreedScheduleJson) {
    return 'AGREED'
  }
  return 'NEGOTIATING'
}

const getWizardMode = (proposal) => {
  const isProposer = proposal.proposerPublicId === authStore.user.publicId
  const hasProposerSchedule = !!proposal.proposerScheduleJson
  const hasReceiverSchedule = !!proposal.receiverScheduleJson

  if (!hasProposerSchedule && !hasReceiverSchedule) {
    return 'propose'
  }

  // [수정] 내가 제시한 적 없고, 상대방만 제시했고, 상대방이 마지막 → 'accept' 모드 (수락 또는 반박 선택)
  // 내가 제시했으면 → 'counter' 모드 (반박)
  const myScheduleJson = isProposer ? proposal.proposerScheduleJson : proposal.receiverScheduleJson
  const opponentScheduleJson = isProposer ? proposal.receiverScheduleJson : proposal.proposerScheduleJson
  const lastSetBy = proposal.lastScheduleSetBy

  if (!myScheduleJson && opponentScheduleJson) {
    return 'accept'  // 내가 미제시 + 상대가 제시 → 수락/반박 선택
  }

  return 'counter'  // 내가 제시했거나, 이미 여러 번 왕복
}

const isTradeCompleted = (proposal) => {
  return proposal.proposerCompletedAt && proposal.receiverCompletedAt
}

const isMyTradeCompleted = (proposal) => {
  const isProposer = proposal.proposerPublicId === authStore.user.publicId
  return isProposer ? !!proposal.proposerCompletedAt : !!proposal.receiverCompletedAt
}

const openDetailModal = async (proposal) => {
  // 아코디언 열 때 해당 proposal의 최신 데이터를 fetch
  try {
    const res = await axios.get(`/api/barter/proposals/${proposal.id}`)
    selectedDetailProposal.value = res.data
  } catch (e) {
    // fetch 실패 시 기존 데이터 사용
    selectedDetailProposal.value = proposal
  }
  isDetailModalOpen.value = true
}

const handleDetailModalSuccess = () => {
  isDetailModalOpen.value = false
  fetchProposals()
}

const getConditionStatusIcon = (proposal) => {
  const status = getScheduleStatus(proposal)
  if (status === 'NO_SCHEDULE') return '⚪'
  if (status === 'AGREED') return '✅'

  // NEGOTIATING 상태
  if (isMyTurn(proposal)) return '🟡'
  if (isConditionMatched(proposal)) return '🟢'
  return '🔴'
}

const getConditionStatusText = (proposal) => {
  const status = getScheduleStatus(proposal)
  if (status === 'NO_SCHEDULE') return '조건 미설정'
  if (status === 'AGREED') return '합의 완료'

  // NEGOTIATING 상태
  if (isMyTurn(proposal)) return '협의 중 (내 차례)'
  if (isConditionMatched(proposal)) return '조건 일치 (수락 가능)'
  return '조건 협의 중'
}

const goToMessage = (proposal) => {
  // 상대방 publicId 계산
  const counterpartyPublicId = proposal.receiverPublicId === authStore.user.publicId ? proposal.proposerPublicId : proposal.receiverPublicId
  // 쪽지함으로 이동하면서 해당 대화를 자동으로 열도록 저장
  sessionStorage.setItem('focusCounterparty', counterpartyPublicId)
  router.push('/keywords?tab=messages')
}

const viewPost = (postId, proposalId) => {
  // 현재 상태를 sessionStorage에 저장 (돌아올 때 복원하기 위해)
  sessionStorage.setItem('barterTabNavState', JSON.stringify({
    fromBarterTab: true,
    proposalId: proposalId,
    filterStatus: filterStatus.value
  }))
  router.push(`/board/post/${postId}`)
}

const scrollToProposal = (proposalId) => {
  // 특정 proposal로 스크롤
  const element = document.querySelector(`[data-proposal-id="${proposalId}"]`)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const toggleExpanded = (proposalId) => {
  if (expandedProposalIds.value.has(proposalId)) {
    expandedProposalIds.value.delete(proposalId)
  } else {
    expandedProposalIds.value.add(proposalId)
  }
}

const isExpanded = (proposalId) => {
  return expandedProposalIds.value.has(proposalId)
}

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${d.getMonth()+1}.${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

// [협상 비교 카드 유틸 함수들]
const parseSchedule = (json) => {
  try {
    return json ? JSON.parse(json) : null
  } catch {
    return null
  }
}

const getMyScheduleJson = (proposal) => {
  const isProposer = proposal.proposerPublicId === authStore.user.publicId
  return isProposer ? proposal.proposerScheduleJson : proposal.receiverScheduleJson
}

const getOpponentScheduleJson = (proposal) => {
  const isProposer = proposal.proposerPublicId === authStore.user.publicId
  return isProposer ? proposal.receiverScheduleJson : proposal.proposerScheduleJson
}

const getMyScheduleForWizard = (proposal) => {
  const json = getMyScheduleJson(proposal)
  return parseSchedule(json)
}

const getOpponentScheduleForWizard = (proposal) => {
  const json = getOpponentScheduleJson(proposal)
  return parseSchedule(json)
}

const isMyTurn = (proposal) => {
  const isProposer = proposal.proposerPublicId === authStore.user.publicId
  const lastSetBy = proposal.lastScheduleSetBy
  if (!lastSetBy) return isProposer
  if (isProposer && lastSetBy === 'RECEIVER') return true
  if (!isProposer && lastSetBy === 'PROPOSER') return true
  return false
}

const getFieldDiff = (mySchedule, opponentSchedule, field) => {
  if (!mySchedule || !opponentSchedule) return 'unknown'

  // 주소 필드는 역할이 교차되므로 다르게 비교
  if (field === 'senderAddress') {
    return mySchedule.senderAddress === opponentSchedule.receiverAddress ? 'same' : 'diff'
  }
  if (field === 'receiverAddress') {
    return mySchedule.receiverAddress === opponentSchedule.senderAddress ? 'same' : 'diff'
  }

  // 그 외 필드는 직접 비교
  return mySchedule[field] === opponentSchedule[field] ? 'same' : 'diff'
}

const isConditionMatched = (proposal) => {
  const my = parseSchedule(getMyScheduleJson(proposal))
  const op = parseSchedule(getOpponentScheduleJson(proposal))
  if (!my || !op) return false

  // 방식 비교
  if (my.method !== op.method) return false

  // 직거래: 장소 비교
  if (my.method === 'DIRECT') {
    return my.location === op.location && my.tradeDateTime === op.tradeDateTime
  }

  // 문고리/택배: 주소는 역할을 교차해서 비교 (내 발송지 === 상대방 수신지)
  return my.senderAddress === op.receiverAddress &&
    my.receiverAddress === op.senderAddress &&
    my.tradeDateTime === op.tradeDateTime
}

const getMethodLabel = (method) => {
  const map = { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }
  return map[method] || method
}

const formatAddressField = (method, address) => {
  if (!address) return '미지정'
  if (address.length > 20) return address.substring(0, 20) + '...'
  return address
}

const formatDateTimeField = (dt) => {
  if (!dt) return '미지정'
  const d = new Date(dt)
  return `${d.getMonth()+1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ✅ fetchProposals 완료 후 알림/쪽지로 온 경우 처리
const processQueryExpand = () => {
  const expandProposalId = route.query.expandProposal

  if (expandProposalId) {
    const proposalId = Number(expandProposalId)
    // ① expandedProposalIds에 추가 (항상)
    expandedProposalIds.value.add(proposalId)

    // ② 렌더링 완료 후 스크롤 (proposals가 있을 때만)
    if (proposals.value.length > 0) {
      // nextTick을 중첩하여 DOM 업데이트 완료 보장
      nextTick(() => {
        nextTick(() => {
          setTimeout(() => {
            scrollToProposal(proposalId)
          }, 500)
        })
      })
    }
  }
}

onMounted(async () => {
  // ✅ 알림(expandProposal)을 우선 처리
  const expandProposalId = route.query.expandProposal

  // 상세페이지에서 돌아온 경우 상태 복원
  const savedState = sessionStorage.getItem('barterTabNavState')
  if (savedState && !expandProposalId) {  // expandProposalId가 없을 때만 상태 복원
    try {
      const { fromBarterTab, proposalId, filterStatus: savedFilterStatus } = JSON.parse(savedState)
      if (fromBarterTab && savedFilterStatus) {
        filterStatus.value = savedFilterStatus
        sessionStorage.removeItem('barterTabNavState')

        await fetchProposals()
        nextTick(() => {
          expandedProposalIds.value.add(Number(proposalId))
          setTimeout(() => {
            scrollToProposal(proposalId)
          }, 300)
        })
        return
      }
    } catch (e) {
    }
  }

  // 데이터 로드
  await fetchProposals()

  // ✅ expandProposalId 우선 처리 (알림, 쪽지함)
  if (expandProposalId) {
    sessionStorage.removeItem('focusTradeProposal')  // sessionStorage 정리
    processQueryExpand()
    return
  }

  // [하이브리드] 쪽지함에서 거래 제안으로 이동한 경우 (fallback)
  const focusProposalId = sessionStorage.getItem('focusTradeProposal')
  if (focusProposalId) {
    sessionStorage.removeItem('focusTradeProposal')
    nextTick(() => {
      expandedProposalIds.value.add(Number(focusProposalId))
      setTimeout(() => {
        scrollToProposal(parseInt(focusProposalId))
      }, 300)
    })
    return
  }
})

// ✅ 알림이나 쪽지에서 온 경우 route query 변경 감지
watch(() => route.query.expandProposal, (newVal) => {
  if (newVal) {
    processQueryExpand()
  }
})

// ✅ 데이터가 나중에 로드되는 경우를 대비해 proposals 감시
watch(() => proposals.value, (newVal) => {
  if (newVal && newVal.length > 0 && route.query.expandProposal) {
    processQueryExpand()
  }
}, { once: true })

// ✅ 같은 페이지에 있을 때 알림 클릭 감지 (notificationStore를 통해)
watch(() => notificationStore.pendingExpandProposalId, (newVal) => {
  if (newVal) {
    const proposalId = Number(newVal)
    expandedProposalIds.value.add(proposalId)
    notificationStore.clearPendingExpandProposal()

    // 데이터가 로드되지 않은 경우 로드 대기
    if (proposals.value.length === 0) {
      watch(() => proposals.value, (proposals) => {
        if (proposals.length > 0) {
          nextTick(() => scrollToProposal(proposalId))
        }
      }, { once: true })
    } else {
      nextTick(() => scrollToProposal(proposalId))
    }
  }
})
</script>

<style scoped>
.barter-tab-container { display: flex; flex-direction: column; gap: 24px; }

.barter-summary-cards { display: flex; gap: 12px; }
.summary-card {
  flex: 1; padding: 16px; border-radius: 16px; border: 1.5px solid var(--border-color);
  background: var(--card-bg); cursor: pointer; transition: all 0.2s;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.summary-card:hover { border-color: var(--link-color); transform: translateY(-2px); }
.summary-card.active { border-color: var(--link-color); background: rgba(0, 149, 246, 0.05); }
.s-label { font-size: 0.8rem; font-weight: 700; color: var(--text-secondary); }
.s-value { font-size: 1.2rem; font-weight: 900; color: var(--text-primary); }

.proposals-list { display: flex; flex-direction: column; gap: 16px; }
.proposal-item-card {
  padding: 20px; border: 1.5px solid var(--border-color); border-radius: 20px;
  background: var(--card-bg); display: flex; flex-direction: column; gap: 16px;
}

.proposal-header { display: flex; align-items: center; gap: 8px; }
.status-chip { padding: 4px 12px; border-radius: 20px; color: white; font-size: 0.75rem; font-weight: 800; }
.my-proposal-chip { padding: 4px 12px; border-radius: 20px; color: white; font-size: 0.75rem; font-weight: 800; background: #8b5cf6; }
.proposal-date { font-size: 0.75rem; color: var(--text-muted); margin-left: auto; }

.trade-info-main { display: flex; align-items: center; gap: 20px; background: var(--hover-bg); padding: 20px; border-radius: 16px; }
.item-box { flex: 1; display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.box-label { font-size: 0.7rem; font-weight: 800; color: var(--text-secondary); text-transform: uppercase; }
.item-content { display: flex; align-items: center; gap: 10px; cursor: pointer; justify-content: space-between; }
.mini-thumb { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
.mini-thumb-right { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
.mini-thumb-placeholder { width: 40px; height: 40px; border-radius: 8px; background: var(--divider-color); display: flex; align-items: center; justify-content: center; font-size: 1.2rem; flex-shrink: 0; }
.p-text-group { display: flex; flex-direction: column; gap: 2px; }
.p-title { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); margin: 0; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.p-item-name { font-size: 0.75rem; color: var(--text-secondary); margin: 0; line-height: 1.2; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.trade-arrow { font-size: 1.2rem; opacity: 0.5; flex-shrink: 0; }
.trade-arrow.pc-icon { display: block; }
.trade-arrow.mobile-icon { display: none; }

.proposal-message { display: flex; gap: 8px; background: rgba(0, 149, 246, 0.05); padding: 16px; border-radius: 12px; }
.m-icon { font-size: 0.9rem; }
.m-text { font-size: 0.85rem; color: var(--text-primary); margin: 0; line-height: 1.5; }

/* [협상 비교 카드] */
.condition-matched-banner { padding: 12px 16px; background: rgba(16, 185, 129, 0.1); border-left: 4px solid #10b981; border-radius: 8px; color: #10b981; font-size: 0.9rem; font-weight: 700; margin-bottom: 16px; }

.negotiation-compare-card { padding: 20px; background: var(--hover-bg); border-radius: 16px; }
.compare-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.compare-title { font-size: 0.95rem; font-weight: 800; color: var(--text-primary); }
.round-info { font-size: 0.8rem; color: var(--text-secondary); font-weight: 600; }

.compare-content { display: grid; grid-template-columns: 1fr auto 1fr; gap: 12px; align-items: center; }
.condition-side { display: flex; flex-direction: column; gap: 12px; }
.side-label { font-size: 0.85rem; font-weight: 800; color: var(--text-primary); }
.condition-fields { display: flex; flex-direction: column; gap: 10px; }
.field-row { display: flex; flex-direction: column; gap: 8px; padding: 8px; border-radius: 8px; background: var(--card-bg); }
.field-label { font-size: 0.75rem; color: var(--text-secondary); font-weight: 700; }
.field-value { font-size: 0.85rem; color: var(--text-primary); font-weight: 700; text-align: left; word-break: break-word; white-space: normal; }
.field-value.same { background: rgba(16, 185, 129, 0.15); padding: 4px; border-radius: 4px; color: #10b981; }
.field-value.diff { background: rgba(255, 165, 0, 0.15); padding: 4px; border-radius: 4px; color: #f97316; }
.condition-empty { font-size: 0.85rem; color: var(--text-muted); text-align: center; padding: 20px 0; }
.compare-divider { display: flex; align-items: center; justify-content: center; font-size: 0.85rem; font-weight: 700; color: var(--text-secondary); }

.proposal-item { border: 1px solid var(--border-color); border-radius: 12px; overflow: hidden; margin-bottom: 12px; }
.proposal-header-accordion {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--hover-bg);
  cursor: pointer;
  transition: background 0.2s;
}
.proposal-header-accordion:hover { background: rgba(59, 130, 246, 0.1); }
.header-left { display: flex; align-items: center; gap: 12px; }
.accordion-toggle {
  font-size: 0.8rem;
  color: var(--text-secondary);
  transition: transform 0.2s;
  display: inline-block;
  width: 16px;
  text-align: center;
}
.accordion-toggle.expanded { transform: rotate(0deg); }
.proposal-body {
  padding: 20px;
  background: var(--card-bg);
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.condition-status-row { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: var(--hover-bg); border-radius: 12px; }
.condition-icon { font-size: 1.2rem; }
.condition-text { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); }

.proposal-actions { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; justify-content: flex-end; }
.btn-message { padding: 10px 14px; border-radius: 10px; font-size: 0.8rem; font-weight: 800; cursor: pointer; transition: all 0.2s; border: 1px solid var(--border-color); background: var(--card-bg); color: var(--text-primary); order: 100; }
.btn-message:hover { transform: translateY(-2px); box-shadow: 0 4px 10px rgba(0,0,0,0.1); }
.btn-detail-view { padding: 10px 14px; border-radius: 10px; font-size: 0.8rem; font-weight: 800; cursor: pointer; transition: all 0.2s; border: 1px solid var(--border-color); background: var(--card-bg); color: var(--text-primary); }
.btn-detail-view:hover { transform: translateY(-2px); box-shadow: 0 4px 10px rgba(0,0,0,0.1); }
.schedule-info { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: rgba(59, 130, 246, 0.1); border-radius: 8px; flex-basis: auto; margin-left: auto; }
.schedule-info.agreed { background: rgba(16, 185, 129, 0.1); }
.info-text { font-size: 0.8rem; font-weight: 700; color: #3b82f6; }
.schedule-info.agreed .info-text { color: #10b981; }
.btn-action {
  padding: 10px 14px; border-radius: 10px; font-size: 0.8rem; font-weight: 800; cursor: pointer; transition: all 0.2s;
  border: 1px solid var(--border-color); background: var(--card-bg); color: var(--text-primary);
}
.btn-action:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 10px rgba(0,0,0,0.1); }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-action.accept { background: #10b981; color: white; border: none; }
.btn-action.reject { color: #ef4444; border-color: #ef4444; }
.btn-action.complete { background: #10b981; color: white; border: none; }
.btn-action.completed { background: #10b981; color: white; border: none; opacity: 0.7; }
.btn-action.wizard { background: #8b5cf6; color: white; border: none; }
.btn-action.accept-schedule { background: #10b981; color: white; border: none; }
.btn-action.counter-schedule { background: #8b5cf6; color: white; border: none; }
.btn-action.counter-schedule:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-action.reject-schedule { color: #ef4444; border-color: #ef4444; }
.btn-action.waiting { background: rgba(107, 114, 128, 0.1); color: var(--text-secondary); border: none; }
.round-limit-notice { font-size: 0.75rem; color: #ef4444; font-weight: 600; width: 100%; text-align: center; }

.my-proposal-label { font-size: 0.8rem; color: var(--text-secondary); font-weight: 600; font-style: italic; }

.empty-state { padding: 60px 0; text-align: center; }
.empty-icon { font-size: 3rem; display: block; margin-bottom: 16px; opacity: 0.5; }

.question-modal-body { padding: 10px 0; }
.q-guide { font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 15px; }
.q-textarea { width: 100%; height: 100px; padding: 12px; border-radius: 12px; border: 1px solid var(--border-color); background: var(--hover-bg); font-size: 0.95rem; color: var(--text-primary); resize: none; outline: none; }
.q-notice { font-size: 0.8rem; color: #d97706; margin-top: 10px; font-weight: 600; }

.modal-footer-btns { display: flex; gap: 12px; justify-content: flex-end; width: 100%; }
.btn-modal-cancel { padding: 12px 24px; border-radius: 10px; border: 1px solid var(--border-color); background: none; font-weight: 700; cursor: pointer; }
.btn-modal-submit { padding: 12px 24px; border-radius: 10px; border: none; background: var(--link-color); color: white; font-weight: 800; cursor: pointer; }
.btn-modal-submit:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 768px) {
  .barter-summary-cards { flex-wrap: wrap; }
  .summary-card { min-width: 100px; }
  .trade-info-main { flex-direction: column; align-items: flex-start; gap: 12px; }
  .trade-arrow.pc-icon { display: none; }
  .trade-arrow.mobile-icon { display: block; align-self: center; }
  .btn-action { flex: 1; text-align: center; padding: 6px 6px; font-size: 0.6rem; }
  .btn-action.wizard { flex: 1; }
  .btn-action.accept-schedule { flex: 1; }
  .btn-action.counter-schedule { flex: 1; }
  .btn-action.reject-schedule { flex: 1; }
  .btn-action.waiting { flex: 1; }
  .btn-message { flex: 1; padding: 6px 6px; font-size: 0.6rem; order: 100; }
  .btn-detail-view { flex: 1; padding: 6px 6px; font-size: 0.6rem; }
  .schedule-info { padding: 8px 10px; font-size: 0.75rem; flex-basis: 100%; }
  .info-text { font-size: 0.75rem; }
  .compare-content { grid-template-columns: 1fr; gap: 16px; }
  .compare-divider { height: auto; margin-bottom: 8px; }
  .round-limit-notice { margin-top: 8px; }
}
</style>
