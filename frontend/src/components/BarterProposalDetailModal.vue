<template>
  <GlobalModal @close="$emit('close')" class="proposal-detail-modal">
    <template #header>
      <div class="detail-header">
        <h3>{{ isReceiver ? '📬 받은 제안' : '📤 보낸 제안' }}</h3>
        <div class="proposal-meta">
          <span class="negotiation-round">협상 {{ proposal.negotiationRound || 1 }}회차</span>
          <span class="proposal-date">{{ formatDate(proposal.createdAt) }}</span>
        </div>
      </div>
    </template>

    <template #body>
      <!-- 1️⃣ 거래 물품 카드 -->
      <div class="items-section">
        <h4 class="section-title">🛍️ 거래 상품</h4>
        <div class="items-card">
          <div class="item-box mine">
            <span class="item-label">나의 상품</span>
            <p class="item-name">{{ getMyItem(proposal).itemName || getMyItem(proposal).title }}</p>
          </div>
          <div class="exchange-icon pc-icon">⇄</div>
          <div class="exchange-icon mobile-icon">⇄</div>
          <div class="item-box opponent">
            <span class="item-label">상대 상품</span>
            <p class="item-name">{{ getOpponentItem(proposal).itemName || getOpponentItem(proposal).title }}</p>
          </div>
        </div>
      </div>

      <!-- 2️⃣ 조건 비교 카드 -->
      <div v-if="proposerInitialSchedule || receiverInitialSchedule || getLatestSchedule()" class="conditions-section">
        <div class="accordion-header" @click="showConditionsAccordion = !showConditionsAccordion">
          <span class="accordion-toggle">{{ showConditionsAccordion ? '▼' : '▶' }}</span>
          <h4 class="section-title">📋 거래 조건</h4>
        </div>

        <!-- 내 조건 vs 상대방 조건 비교 (항목별 수평 비교) -->
        <div v-if="showConditionsAccordion" class="accordion-body">
          <div class="conditions-comparison-container">
          <!-- 거래 방식 비교 -->
          <div class="item-comparison" :class="{ matched: methodMatched === true, mismatched: methodMatched === false }">
            <div class="my-item" :class="{ matched: methodMatched === true, mismatched: methodMatched === false, 'no-value': !myConditionToShow?.method }">
              <div class="item-header">
                <span class="badge">나</span>
                <span v-if="myLatestToShow && !opponentLatestToShow" class="badge-latest">최신</span>
              </div>
              <div class="item-content" :class="{ matched: methodMatched === true, mismatched: methodMatched === false, 'no-value': !myConditionToShow?.method }">
                <span class="icon">{{ getMethodIcon(myConditionToShow?.method || 'DIRECT') }}</span>
                <div>
                  <p class="label">거래 방식</p>
                  <p class="value" :class="{ 'no-value': !myConditionToShow?.method }">{{ getMethodLabel(myConditionToShow?.method) || '미지정' }}</p>
                </div>
              </div>
            </div>
            <div class="opponent-item" :class="{ matched: methodMatched === true, mismatched: methodMatched === false, 'no-value': !opponentConditionToShow?.method }">
              <div class="item-header">
                <span class="badge">상대방</span>
                <span v-if="opponentLatestToShow && !myLatestToShow" class="badge-latest">최신</span>
              </div>
              <div class="item-content" :class="{ matched: methodMatched === true, mismatched: methodMatched === false, 'no-value': !opponentConditionToShow?.method }">
                <span class="icon">{{ getMethodIcon(opponentConditionToShow?.method) }}</span>
                <div>
                  <p class="label">거래 방식</p>
                  <p class="value" :class="{ 'no-value': !opponentConditionToShow?.method }">{{ getMethodLabel(opponentConditionToShow?.method) || '미지정' }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 거래 날짜 비교 -->
          <div class="item-comparison" :class="{ matched: dateMatched === true, mismatched: dateMatched === false }">
            <div class="my-item" :class="{ matched: dateMatched === true, mismatched: dateMatched === false, 'no-value': !myConditionToShow?.tradeDateTime }">
              <div class="item-header">
                <span class="badge">나</span>
              </div>
              <div class="item-content" :class="{ matched: dateMatched === true, mismatched: dateMatched === false, 'no-value': !myConditionToShow?.tradeDateTime }">
                <span class="icon">📅</span>
                <div>
                  <p class="label">거래 날짜</p>
                  <p class="value" :class="{ 'no-value': !myConditionToShow?.tradeDateTime }">{{ formatDateTime(myConditionToShow?.tradeDateTime) || '미지정' }}</p>
                </div>
              </div>
            </div>
            <div class="opponent-item" :class="{ matched: dateMatched === true, mismatched: dateMatched === false, 'no-value': !opponentConditionToShow?.tradeDateTime }">
              <div class="item-header">
                <span class="badge">상대방</span>
              </div>
              <div class="item-content" :class="{ matched: dateMatched === true, mismatched: dateMatched === false, 'no-value': !opponentConditionToShow?.tradeDateTime }">
                <span class="icon">📅</span>
                <div>
                  <p class="label">거래 날짜</p>
                  <p class="value" :class="{ 'no-value': !opponentConditionToShow?.tradeDateTime }">{{ formatDateTime(opponentConditionToShow?.tradeDateTime) || '미지정' }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 거래 위치/배송 정보 비교 (통합) -->
          <div v-if="myConditionToShow?.method" class="item-comparison" :class="{ matched: locationMatched === true, mismatched: locationMatched !== true }">
            <div class="my-item" :class="{ matched: locationMatched === true, mismatched: locationMatched !== true, 'no-value': !hasLocationValue(myConditionToShow) }">
              <div class="item-header">
                <span class="badge">나</span>
              </div>
              <div class="item-content" :class="{ matched: locationMatched === true, mismatched: locationMatched !== true, 'no-value': !hasLocationValue(myConditionToShow) }">
                <span class="icon">{{ getLocationIcon(myConditionToShow.method) }}</span>
                <div>
                  <p class="label">{{ getLocationLabel(myConditionToShow.method) }}</p>
                  <template v-if="myConditionToShow.method === 'PARCEL'">
                    <p class="value">발송지 : {{ getLocationValue(myConditionToShow).sender || '미지정' }}</p>
                    <p class="value">수신지 : {{ getLocationValue(myConditionToShow).receiver || '미지정' }}</p>
                  </template>
                  <p v-else class="value" :class="{ 'no-value': !hasLocationValue(myConditionToShow) }">{{ getLocationValue(myConditionToShow) }}</p>
                </div>
              </div>
            </div>
            <div class="opponent-item" :class="{ matched: locationMatched === true, mismatched: locationMatched !== true, 'no-value': !hasLocationValue(opponentConditionToShow) }">
              <div class="item-header">
                <span class="badge">상대방</span>
              </div>
              <div class="item-content" :class="{ matched: locationMatched === true, mismatched: locationMatched !== true, 'no-value': !hasLocationValue(opponentConditionToShow) }">
                <span class="icon">{{ getLocationIcon(opponentConditionToShow?.method) }}</span>
                <div>
                  <p class="label">{{ getLocationLabel(opponentConditionToShow?.method) }}</p>
                  <template v-if="opponentConditionToShow?.method === 'PARCEL'">
                    <p class="value">발송지 : {{ getLocationValue(opponentConditionToShow).sender || '미지정' }}</p>
                    <p class="value">수신지 : {{ getLocationValue(opponentConditionToShow).receiver || '미지정' }}</p>
                  </template>
                  <p v-else class="value" :class="{ 'no-value': !hasLocationValue(opponentConditionToShow) }">{{ getLocationValue(opponentConditionToShow) }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 메시지 표시 -->
          <div v-if="myConditionToShow?.message || opponentConditionToShow?.message" class="messages-section">
            <h5 class="messages-title">💬 거래 메시지</h5>
            <div v-if="myConditionToShow?.message" class="message-row my-message">
              <span class="badge">나</span>
              <p class="message-text">{{ myConditionToShow.message }}</p>
            </div>
            <div v-if="opponentConditionToShow?.message" class="message-row opponent-message">
              <span class="badge">상대방</span>
              <p class="message-text">{{ opponentConditionToShow.message }}</p>
            </div>
          </div>
          </div>

          <!-- 최종 합의 조건 (3개 항목 모두 일치할 때만) -->
          <div v-if="currentConditionMatch === 'matched' && proposal.finalMethod" class="condition-card agreed-card">
            <div class="card-header agreed">
              <span class="badge agreed-badge">✅ 최종 합의 완료</span>
            </div>
            <div class="card-body">
              <div class="condition-row">
                <span class="icon">{{ getMethodIcon(proposal.finalMethod) }}</span>
                <div>
                  <span class="label">거래 방식</span>
                  <p class="value">{{ getMethodLabel(proposal.finalMethod) }}</p>
                </div>
              </div>
              <div class="condition-row">
                <span class="icon">📅</span>
                <div>
                  <span class="label">거래 일시</span>
                  <p class="value">{{ formatDateTime(proposal.finalTradeDateTime) }}</p>
                </div>
              </div>
              <!-- PARCEL: 배송 정보 -->
              <div v-if="proposal.finalMethod === 'PARCEL'" class="condition-row">
                <span class="icon">📍</span>
                <div>
                  <span class="label">배송</span>
                  <p class="value">발송지 : {{ getFinalSender() }}</p>
                  <p class="value">수신지 : {{ getFinalReceiver() }}</p>
                </div>
              </div>
              <!-- DIRECT/DOORSTEP: 거래 장소 -->
              <div v-if="(proposal.finalMethod === 'DIRECT' || proposal.finalMethod === 'DOORSTEP') && proposal.finalLocation" class="condition-row">
                <span class="icon">📌</span>
                <div>
                  <span class="label">거래 장소</span>
                  <p class="value">{{ proposal.finalLocation }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 협의 진행 중 메시지 (조건 불일치 시) -->
        <div v-if="getScheduleStatus(proposal) === 'NEGOTIATING' && currentConditionMatch !== 'matched'" class="negotiation-status-message">
          <div class="status-content">
            <span class="status-icon">⏳</span>
            <div>
              <p class="status-text">거래협의중</p>
              <p class="status-desc">3개 항목이 모두 일치하면 최종 합의가 완료됩니다</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 3️⃣ 협상 진행 과정 (아코디언) -->
      <div v-if="getScheduleStatus(proposal) !== 'NO_SCHEDULE'" class="negotiation-history-section">
        <div class="accordion-header" @click="showHistoryAccordion = !showHistoryAccordion">
          <span class="accordion-toggle">{{ showHistoryAccordion ? '▼' : '▶' }}</span>
          <h4 class="section-title">📊 협상 진행 과정 ({{ negotiationHistory.length }}회차)</h4>
        </div>

        <div v-if="showHistoryAccordion" class="accordion-body">
          <div v-for="history in negotiationHistory" :key="history.id" class="history-item" :class="{ 'is-me': (history.setBy === 'PROPOSER' && !isReceiver) || (history.setBy === 'RECEIVER' && isReceiver) }">
            <div class="round-header">
              <span class="round-badge">협상 {{ history.round }}회차</span>
              <span v-if="history.setBy === 'PROPOSER'" class="actor-badge proposer" :class="{ 'is-me': !isReceiver, 'is-opponent': isReceiver }">📤 {{ isReceiver ? '상대가' : '내가' }}</span>
              <span v-else class="actor-badge receiver" :class="{ 'is-me': isReceiver, 'is-opponent': !isReceiver }">📬 {{ isReceiver ? '내가' : '상대가' }}</span>
              <span class="created-time">{{ formatDate(history.createdAt) }}</span>
            </div>

            <div class="history-details">
              <div class="detail-row">
                <span class="detail-label">거래 방식:</span>
                <span class="detail-value">{{ getMethodLabel(history.method) }}</span>
              </div>
              <!-- DIRECT/DOORSTEP: 거래 장소 -->
              <div v-if="(history.method === 'DIRECT' || history.method === 'DOORSTEP') && history.location" class="detail-row">
                <span class="detail-label">거래 장소:</span>
                <span class="detail-value">{{ history.location }}</span>
              </div>
              <!-- PARCEL: 발송지/수신지 -->
              <div v-if="history.method === 'PARCEL'" class="detail-row">
                <span class="detail-label">발송지:</span>
                <span class="detail-value">{{ getHistorySender(history) || '-' }}</span>
              </div>
              <div v-if="history.method === 'PARCEL'" class="detail-row">
                <span class="detail-label">수신지:</span>
                <span class="detail-value">{{ getHistoryReceiver(history) || '-' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">거래 시간:</span>
                <span class="detail-value">{{ formatDateTime(history.tradeDateTime) }}</span>
              </div>
              <div v-if="history.message" class="detail-row">
                <span class="detail-label">메시지:</span>
                <span class="detail-value">{{ history.message }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 거래 거절 안내 -->
      <div v-if="proposal.status === 'REJECTED'" class="rejection-notice">
        <div class="notice-content">
          <span class="notice-icon">❌</span>
          <p class="notice-text">거래가 거절되었습니다.</p>
        </div>
      </div>

      <!-- 협상 결렬 안내 (10회 자동 취소) -->
      <div v-if="(proposal.negotiationRound || 0) >= 10 && proposal.status === 'CANCELLED'" class="cancellation-notice">
        <div class="notice-content">
          <span class="notice-icon">🚫</span>
          <p class="notice-text">협상 10회 완료로 자동 취소되었습니다.</p>
        </div>
      </div>

      <!-- 최종 합의 완료 안내 -->
      <div v-if="getScheduleStatus(proposal) === 'ACCEPTED'" class="agreement-notice">
        <div class="notice-content">
          <span class="notice-icon">⚠️</span>
          <p class="notice-text">실제 상품 교환 후 아래 거래 완료 버튼을 누르시거나 상품 교환 전 거래취소를 눌러 거래를 취소하실 수 있습니다.</p>
        </div>
      </div>

      <!-- 4️⃣ 거래 완료 상태 -->
      <div v-if="proposal.agreedScheduleJson && proposal.status === 'ACCEPTED'" class="completion-section">
        <h4 class="section-title">✅ 거래 완료 확인</h4>
        <div class="completion-status">
          <div class="status-item" :class="{ completed: proposal.proposerCompletedAt }">
            <span class="status-icon">{{ proposal.proposerCompletedAt ? '✅' : '⏳' }}</span>
            <div>
              <span class="status-label">제안자 확인</span>
              <p class="status-time">{{ proposal.proposerCompletedAt ? formatDate(proposal.proposerCompletedAt) : '대기 중' }}</p>
            </div>
          </div>
          <div class="status-item" :class="{ completed: proposal.receiverCompletedAt }">
            <span class="status-icon">{{ proposal.receiverCompletedAt ? '✅' : '⏳' }}</span>
            <div>
              <span class="status-label">수신자 확인</span>
              <p class="status-time">{{ proposal.receiverCompletedAt ? formatDate(proposal.receiverCompletedAt) : '대기 중' }}</p>
            </div>
          </div>
        </div>
        <button v-if="!isMyCompletion && !proposal.finalCompletedAt" class="btn-complete" @click="handleMarkComplete">
          거래 완료 확인
        </button>
      </div>

      <!-- 5️⃣ 제안 메시지 -->
      <div v-if="proposal.message" class="message-section">
        <h4 class="section-title">💬 제안 메시지</h4>
        <div class="message-box">
          <p>{{ proposal.message }}</p>
        </div>
      </div>

      <!-- 6️⃣ 액션 버튼 -->
      <div class="action-buttons">
        <!-- REJECTED (거래 거절) - 가장 먼저 체크 -->
        <template v-if="proposal.status === 'REJECTED'">
          <button class="btn btn-secondary" @click="$emit('close')">
            닫기
          </button>
        </template>

        <!-- CANCELLED (협상 결렬 - 자동 또는 수동) -->
        <template v-else-if="proposal.status === 'CANCELLED' || (proposal.negotiationRound || 0) >= 10">
          <button class="btn btn-secondary" @click="$emit('close')">
            닫기
          </button>
        </template>

        <!-- NO_SCHEDULE (조건 미설정) -->
        <template v-else-if="getScheduleStatus(proposal) === 'NO_SCHEDULE'">
          <button class="btn btn-primary" @click="openWizard('propose')">
            ⚙️ 조건 제시
          </button>
          <button class="btn btn-danger" @click="respondProposal(proposal.id, 'REJECTED')">
            ❌ 거래 취소
          </button>
        </template>

        <!-- NEGOTIATING (협의중) - 내 차례 O -->
        <template v-else-if="getScheduleStatus(proposal) === 'NEGOTIATING' && isMyTurn">
          <button v-if="isConditionMatched" class="btn btn-primary" @click="acceptSchedule(proposal.id)">
            ✅ 거래 수락
          </button>
          <button v-if="(proposal.negotiationRound || 0) < 10" class="btn btn-secondary" @click="openWizard('counter')">
            ✏️ 조건 변경
          </button>
          <button v-else disabled class="btn btn-secondary" :title="`협상은 최대 10회차까지만 가능합니다 (현재: ${proposal.negotiationRound}/10)`">
            ✏️ 조건 변경 (최대 초과)
          </button>
          <button class="btn btn-danger" @click="respondProposal(proposal.id, 'REJECTED')">
            ❌ 거래 취소
          </button>
        </template>

        <!-- NEGOTIATING (협의중) - 내 차례 X -->
        <template v-else-if="getScheduleStatus(proposal) === 'NEGOTIATING' && !isMyTurn && (proposal.negotiationRound || 0) < 10">
          <button disabled class="btn btn-secondary">
            ⏳ 상대방 응답 대기 중
          </button>
        </template>

        <!-- ACCEPTED (수락완료 - 거래 진행 대기) -->
        <template v-else-if="getScheduleStatus(proposal) === 'ACCEPTED'">
          <button class="btn btn-primary" @click="completeTrade(proposal.id)">
            ✅ 거래 완료
          </button>
          <button class="btn btn-danger" @click="cancelTrade(proposal.id)">
            🚫 거래 취소
          </button>
        </template>

        <!-- AGREED (합의완료) -->
        <template v-else-if="getScheduleStatus(proposal) === 'AGREED'">
          <button v-if="!proposal.finalCompletedAt && !isMyCompletion" class="btn btn-primary" @click="completeTrade(proposal.id)">
            거래 완료
          </button>
          <button v-else-if="!proposal.finalCompletedAt && isMyCompletion" disabled class="btn btn-secondary">
            ⏳ 상대방 확인 중
          </button>
          <button v-else disabled class="btn btn-success">
            ✅ 거래 완료됨
          </button>
          <button class="btn btn-secondary" @click="$emit('close')">
            닫기
          </button>
        </template>

        <!-- 기타 -->
        <button v-else class="btn btn-secondary" @click="$emit('close')">
          닫기
        </button>
      </div>

      <!-- TradeWizard 모달 (내부 연동) -->
      <TradeWizard
        v-if="isWizardOpen"
        :proposal="proposal"
        :mode="wizardMode"
        :my-existing-schedule="getMyScheduleForModal()"
        :opponent-existing-schedule="getOpponentScheduleForModal()"
        @close="isWizardOpen = false"
        @success="handleWizardSuccess"
      />
    </template>
  </GlobalModal>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import axios from '@/plugins/axios'
import GlobalModal from '@/components/common/GlobalModal.vue'
import TradeWizard from '@/components/TradeWizard.vue'

const props = defineProps({
  proposal: { type: Object, required: true }
})

const emit = defineEmits(['close', 'success'])

const authStore = useAuthStore()
const uiStore = useUiStore()
const showScheduleModal = ref(false)
const isWizardOpen = ref(false)
const wizardMode = ref('propose')
const negotiationHistory = ref([])
const showHistoryAccordion = ref(false)
const showConditionsAccordion = ref(true)

// 계산된 속성들
const isReceiver = computed(() => props.proposal.receiverPublicId === authStore.user.publicId)

const isMyCompletion = computed(() => {
  if (isReceiver.value) {
    return !!props.proposal.receiverCompletedAt
  } else {
    return !!props.proposal.proposerCompletedAt
  }
})

// [초기 선호 조건] round 1 (PROPOSER), round 2 (RECEIVER)
const proposerInitialSchedule = computed(() => {
  const round1 = negotiationHistory.value.find(h => h.round === 1 && h.setBy === 'PROPOSER')
  return round1 ? {
    method: round1.method,
    location: round1.location,
    tradeDateTime: round1.tradeDateTime,
    senderAddress: round1.senderAddress,
    receiverAddress: round1.receiverAddress,
    message: round1.message
  } : null
})

const receiverInitialSchedule = computed(() => {
  const round2 = negotiationHistory.value.find(h => h.round === 2 && h.setBy === 'RECEIVER')
  return round2 ? {
    method: round2.method,
    location: round2.location,
    tradeDateTime: round2.tradeDateTime,
    senderAddress: round2.senderAddress,
    receiverAddress: round2.receiverAddress,
    message: round2.message
  } : null
})

// [협상 중인 최신 조건] round >= 3 중 최신 (setBy별로)
const proposerLatestSchedule = computed(() => {
  const rounds = negotiationHistory.value.filter(h => h.round >= 3 && h.setBy === 'PROPOSER').sort((a, b) => b.round - a.round)
  if (rounds.length === 0) return null
  const latest = rounds[0]
  return {
    method: latest.method,
    location: latest.location,
    tradeDateTime: latest.tradeDateTime,
    senderAddress: latest.senderAddress,
    receiverAddress: latest.receiverAddress,
    message: latest.message
  }
})

const receiverLatestSchedule = computed(() => {
  const rounds = negotiationHistory.value.filter(h => h.round >= 3 && h.setBy === 'RECEIVER').sort((a, b) => b.round - a.round)
  if (rounds.length === 0) return null
  const latest = rounds[0]
  return {
    method: latest.method,
    location: latest.location,
    tradeDateTime: latest.tradeDateTime,
    senderAddress: latest.senderAddress,
    receiverAddress: latest.receiverAddress,
    message: latest.message
  }
})

// [표시할 조건] 협상 중이면 최신, 아니면 초기
const proposerConditionToShow = computed(() => {
  return proposerLatestSchedule.value || proposerInitialSchedule.value
})

const receiverConditionToShow = computed(() => {
  return receiverLatestSchedule.value || receiverInitialSchedule.value
})

// [내 기준 / 상대방 기준 재정렬]
const myConditionToShow = computed(() => {
  return isReceiver.value ? receiverConditionToShow.value : proposerConditionToShow.value
})

const opponentConditionToShow = computed(() => {
  return isReceiver.value ? proposerConditionToShow.value : receiverConditionToShow.value
})

const myLatestToShow = computed(() => {
  return isReceiver.value ? receiverLatestSchedule.value : proposerLatestSchedule.value
})

const opponentLatestToShow = computed(() => {
  return isReceiver.value ? proposerLatestSchedule.value : receiverLatestSchedule.value
})

// [협상 중인 조건] round >= 3 중 최신
const latestProposalSchedule = computed(() => {
  const rounds = negotiationHistory.value.filter(h => h.round >= 3).sort((a, b) => b.round - a.round)
  if (rounds.length === 0) return null
  const latest = rounds[0]
  return {
    method: latest.method,
    location: latest.location,
    tradeDateTime: latest.tradeDateTime,
    senderAddress: latest.senderAddress,
    receiverAddress: latest.receiverAddress
  }
})

const shouldShowNegotiationButtons = computed(() => {
  // round 3 이상이 없으면 협상 버튼 표시
  return negotiationHistory.value.filter(h => h.round >= 3).length === 0
})

const lastProposerLabel = computed(() => {
  if (!props.proposal.lastScheduleSetBy) {
    return isReceiver.value ? '상대' : '나'
  }
  return props.proposal.lastScheduleSetBy === 'RECEIVER' ? (isReceiver.value ? '내' : '상대') : (isReceiver.value ? '상대' : '내')
})

// 함수들
const getOpponentItem = (proposal) => {
  return isReceiver.value ? proposal.offeredPost : proposal.barterPost
}

const getMyItem = (proposal) => {
  return isReceiver.value ? proposal.barterPost : proposal.offeredPost
}

const getMyPreference = () => {
  const item = getMyItem(props.proposal)
  return {
    method: item.preferredMethod || 'DIRECT',
    date: item.preferredDate,
    time: item.preferredTime
  }
}

const getLatestSchedule = () => {
  return latestProposalSchedule.value
}

const getScheduleStatus = (proposal) => {
  // round 3 이상이 없으면 초기 선호 상태
  if (negotiationHistory.value.filter(h => h.round >= 3).length === 0) {
    return 'NO_SCHEDULE'
  }
  // 거래 완료됨
  if (proposal.finalCompletedAt) {
    return 'AGREED'
  }
  // 양쪽이 모두 수락한 상태 (거래 진행 대기)
  if (proposal.status === 'ACCEPTED') {
    return 'ACCEPTED'
  }
  // 협상 취소됨 (10회 초과 등)
  if (proposal.status === 'CANCELLED') {
    return 'CANCELLED'
  }
  // 협상 중
  return 'NEGOTIATING'
}

const getMethodIcon = (method) => {
  const icons = {
    DIRECT: '🤝',
    DOORSTEP: '🚚',
    PARCEL: '📦'
  }
  return icons[method] || '🔄'
}

const getMethodLabel = (method) => {
  const labels = {
    DIRECT: '직거래 (만나서)',
    DOORSTEP: '문고리 거래',
    PARCEL: '택배 배송'
  }
  return labels[method] || method
}

const getLocationIcon = (method) => {
  if (method === 'PARCEL') return '📍'
  return '📌' // DIRECT, DOORSTEP
}

const getLocationLabel = (method) => {
  if (method === 'PARCEL') return '배송정보'
  return '거래장소' // DIRECT, DOORSTEP
}

const getLocationValue = (condition) => {
  if (!condition) return '미지정'

  // PARCEL: 배송 정보
  if (condition.method === 'PARCEL') {
    if (!condition.senderAddress && !condition.receiverAddress) return '미지정'

    const sender = condition.senderAddress
    const receiver = condition.receiverAddress

    return {
      sender: sender || '미지정',
      receiver: receiver || '미지정'
    }
  }

  // DOORSTEP: 물품 놓을 장소
  if (condition.method === 'DOORSTEP') {
    return condition.location || '미지정'
  }

  // DIRECT: 거래 장소
  return condition.location || '미지정'
}

const hasLocationValue = (condition) => {
  if (!condition) return false
  if (condition.method === 'DIRECT') return !!condition.location
  if (condition.method === 'DOORSTEP') return !!condition.location
  if (condition.method === 'PARCEL') return !!(condition.senderAddress || condition.receiverAddress)
  return false
}

// 협상 이력에서 발송자/수신자를 내 기준으로 표시할 때 역전 필요 여부
const shouldSwapHistoryAddresses = (historySetBy) => {
  // PROPOSER가 설정했는데 내가 receiver면: 상대방 기준이므로 역전
  // RECEIVER가 설정했는데 내가 proposer면: 상대방 기준이므로 역전
  return (historySetBy === 'PROPOSER') !== !isReceiver.value
}

// 협상 이력의 발송자를 표시
const getHistorySender = (history) => {
  // DOORSTEP: location 사용
  if (history.method === 'DOORSTEP') {
    return history.location
  }

  // PARCEL: address 사용 (역전 가능)
  if (shouldSwapHistoryAddresses(history.setBy)) {
    return history.receiverAddress
  }
  return history.senderAddress
}

// 협상 이력의 수신자를 표시
const getHistoryReceiver = (history) => {
  // DOORSTEP: location 사용
  if (history.method === 'DOORSTEP') {
    return history.location
  }

  // PARCEL: address 사용 (역전 가능)
  if (shouldSwapHistoryAddresses(history.setBy)) {
    return history.senderAddress
  }
  return history.receiverAddress
}

// 최종 합의의 발송자를 표시 (내 기준)
const getFinalSender = () => {
  // DOORSTEP: location 사용
  if (props.proposal.finalMethod === 'DOORSTEP') {
    return props.proposal.finalLocation
  }

  // PARCEL: address 사용 (역전 가능)
  if (shouldSwapHistoryAddresses(props.proposal.lastScheduleSetBy)) {
    return props.proposal.finalReceiverAddress
  }
  return props.proposal.finalSenderAddress
}

// 최종 합의의 수신자를 표시 (내 기준)
const getFinalReceiver = () => {
  // DOORSTEP: location 사용 (발송자와 동일)
  if (props.proposal.finalMethod === 'DOORSTEP') {
    return props.proposal.finalLocation
  }

  // PARCEL: address 사용 (역전 가능)
  if (shouldSwapHistoryAddresses(props.proposal.lastScheduleSetBy)) {
    return props.proposal.finalSenderAddress
  }
  return props.proposal.finalReceiverAddress
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const formatDateOnly = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()}`
}

const formatDateTime = (val) => {
  if (!val) return '미지정'
  const d = new Date(val)
  return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 일치 여부 판단
const isMyTurn = computed(() => {
  const lastSetBy = props.proposal.lastScheduleSetBy
  if (!lastSetBy) return isReceiver.value ? false : true  // 제안자가 먼저 제시
  if (isReceiver.value && lastSetBy === 'PROPOSER') return true
  if (!isReceiver.value && lastSetBy === 'RECEIVER') return true
  return false
})

// 현재 표시 중인 조건 (초기 또는 협상)의 일치 여부
// [항목별 일치 여부]
const methodMatched = computed(() => {
  const p = myConditionToShow.value
  const r = opponentConditionToShow.value
  if (!p || !r) return null
  return p.method === r.method
})

const dateMatched = computed(() => {
  const p = myConditionToShow.value
  const r = opponentConditionToShow.value
  if (!p || !r) return null
  return p.tradeDateTime === r.tradeDateTime
})

const locationMatched = computed(() => {
  const p = myConditionToShow.value
  const r = opponentConditionToShow.value

  // 한쪽이 없으면 비교 불가
  if (!p || !r) return null

  // 방식이 다르면 불일치
  if (p.method !== r.method) return null

  // 직거래: 장소가 같아야 함
  if (p.method === 'DIRECT') {
    return p.location === r.location
  }

  // 당일배송: 양쪽 모두 당일배송 선택했으면 일치 (이미 method 체크됨)
  if (p.method === 'DOORSTEP') {
    return true
  }

  // 택배: 양쪽 모두 주소를 제시했고 교차 일치해야 함
  if (p.method === 'PARCEL') {
    const myHasAddresses = !!(p.senderAddress && p.receiverAddress)
    const oppHasAddresses = !!(r.senderAddress && r.receiverAddress)
    // 한쪽이라도 주소를 제시하지 않았으면 불일치
    if (!myHasAddresses || !oppHasAddresses) return false
    // 교차 일치 확인 (내 발송지 = 상대방 수신지)
    return p.senderAddress === r.receiverAddress && p.receiverAddress === r.senderAddress
  }

  return null
})

const currentConditionMatch = computed(() => {
  const p = proposerConditionToShow.value
  const r = receiverConditionToShow.value

  // 한쪽이 없으면 불완전
  if (!p || !r) return 'incomplete'

  // 거래 방식 불일치
  if (p.method !== r.method) return 'mismatch'

  // 시간 불일치
  if (p.tradeDateTime !== r.tradeDateTime) return 'mismatch'

  // 직거래: 장소까지 일치해야 함
  if (p.method === 'DIRECT') {
    return p.location === r.location ? 'matched' : 'mismatch'
  }

  // 택배/문고리: 거래 방식과 시간이 일치하면 OK (주소는 역할이 다름)
  return 'matched'
})

const isConditionMatched = computed(() => {
  const my = parsedMySchedule.value
  const op = parsedOpponentSchedule.value
  if (!my || !op) return false

  if (my.method !== op.method) return false

  if (my.method === 'DIRECT') {
    return my.location === op.location && my.tradeDateTime === op.tradeDateTime
  }

  // 주소는 역할이 교차 (내 발송지 === 상대방 수신지)
  return my.senderAddress === op.receiverAddress &&
    my.receiverAddress === op.senderAddress &&
    my.tradeDateTime === op.tradeDateTime
})

const parsedMySchedule = computed(() => {
  // 내가 제시한 최신 조건 (round 3+)
  if (isReceiver.value) {
    // 수신자: receiver round 찾기
    const myRounds = negotiationHistory.value.filter(h => h.round >= 3 && h.setBy === 'RECEIVER').sort((a, b) => b.round - a.round)
    return myRounds.length > 0 ? myRounds[0] : null
  } else {
    // 제안자: proposer round 찾기
    const myRounds = negotiationHistory.value.filter(h => h.round >= 3 && h.setBy === 'PROPOSER').sort((a, b) => b.round - a.round)
    return myRounds.length > 0 ? myRounds[0] : null
  }
})

const parsedOpponentSchedule = computed(() => {
  // 상대가 제시한 최신 조건 (round 3+)
  if (isReceiver.value) {
    // 수신자: proposer round 찾기
    const oppRounds = negotiationHistory.value.filter(h => h.round >= 3 && h.setBy === 'PROPOSER').sort((a, b) => b.round - a.round)
    return oppRounds.length > 0 ? oppRounds[0] : null
  } else {
    // 제안자: receiver round 찾기
    const oppRounds = negotiationHistory.value.filter(h => h.round >= 3 && h.setBy === 'RECEIVER').sort((a, b) => b.round - a.round)
    return oppRounds.length > 0 ? oppRounds[0] : null
  }
})

// TradeWizard 연동
const getWizardMode = () => {
  const hasMySchedule = !!parsedMySchedule.value
  const hasOpponentSchedule = !!parsedOpponentSchedule.value

  if (!hasMySchedule && !hasOpponentSchedule) {
    return 'propose'
  }

  if (!hasMySchedule && hasOpponentSchedule) {
    return 'accept'
  }

  return 'counter'
}

const openWizard = (mode) => {
  wizardMode.value = mode || getWizardMode()
  isWizardOpen.value = true
}

const handleWizardSuccess = async () => {
  isWizardOpen.value = false
  await fetchNegotiationHistory()
  emit('success')
}

const getMyScheduleForModal = () => {
  return parsedMySchedule.value
}

const getOpponentScheduleForModal = () => {
  return parsedOpponentSchedule.value
}

// API 함수들
const acceptSchedule = async (id) => {
  try {
    await axios.post(`/api/barter/proposals/${id}/schedule/accept`)
    await uiStore.showAlert('거래 조건을 수락했습니다!', '완료')
    emit('success')
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '수락 처리에 실패했습니다.', '오류')
  }
}

const completeTrade = async (id) => {
  // 이미 완료한 경우
  if (isMyCompletion.value) {
    await uiStore.showAlert('⏳ 이미 거래 완료 처리를 하였습니다. 상대방의 거래 완료 처리 후 Exp가 수령됩니다.', '거래 완료 대기중')
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
    await axios.post(`/api/barter/proposals/${id}/complete`)
    await uiStore.showAlert('당신의 거래 완료가 확인되었습니다.', '완료')
    emit('success')
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '거래 완료 처리에 실패했습니다.', '오류')
  }
}

const cancelTrade = async (id) => {
  const isConfirmed = await uiStore.showConfirm(
    '거래를 취소하시겠어요?\n\n취소하면 다시 협상할 수 없습니다.',
    '거래 취소',
    '취소',
    '계속하기'
  )
  if (!isConfirmed) return

  try {
    await axios.post(`/api/barter/proposals/${id}/cancel`)
    await uiStore.showAlert('거래가 취소되었습니다.', '완료')
    emit('success')
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '취소 처리에 실패했습니다.', '오류')
  }
}

const respondProposal = async (id, status) => {
  try {
    await axios.post(`/api/barter/proposals/${id}/respond`, null, { params: { status } })
    await uiStore.showAlert('거절 처리되었습니다.', '완료')
    emit('success')
  } catch (e) {
    await uiStore.showAlert('처리에 실패했습니다.', '오류')
  }
}

const handleMarkComplete = async () => {
  // 거래 완료 처리 로직
  await uiStore.showAlert('거래 완료 확인되었습니다!')
}

// 협상 이력 조회
const fetchNegotiationHistory = async () => {
  try {
    const res = await axios.get(`/api/barter/proposals/${props.proposal.id}/history`)
    negotiationHistory.value = res.data
  } catch (e) {
    console.error('협상 이력 조회 실패:', e)
  }
}

// 마운트 시 협상 이력 조회
onMounted(() => {
  fetchNegotiationHistory()
})
</script>

<style scoped>
.proposal-detail-modal {
  --primary-color: #3b82f6;
  --secondary-color: #f97316;
  --success-color: #10b981;
  --my-card-bg: #eff6ff;
  --opponent-card-bg: #fef3c7;
  --agreed-card-bg: #ecfdf5;
}

/* PC에서 모달 너비 확대 */
:deep(.global-modal-card.custom-modal) {
  max-width: 900px;
}

@media (max-width: 1024px) {
  :deep(.global-modal-card.custom-modal) {
    max-width: 700px;
  }
}

@media (max-width: 768px) {
  :deep(.global-modal-card.custom-modal) {
    max-width: 500px;
  }
}

:global(.dark-mode) .proposal-detail-modal {
  --my-card-bg: #1e3a5f;
  --opponent-card-bg: #3f3a1f;
  --agreed-card-bg: #1a3a2a;
}

.detail-header {
  padding: 0;
}

.detail-header h3 {
  margin: 0 0 12px 0;
  font-size: 1.2rem;
  font-weight: 800;
}

.proposal-meta {
  display: flex;
  gap: 16px;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.negotiation-round {
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary-color);
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 700;
}

/* 거래 물품 섹션 */
.items-section {
  margin-bottom: 24px;
}

.items-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--hover-bg);
  border-radius: 12px;
  justify-content: center;
  width: 100%;
  box-sizing: border-box;
}

.item-box {
  flex: 1;
  min-width: 0;
  padding: 12px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-box.opponent {
  background: var(--my-card-bg);
  border-left: 3px solid var(--primary-color);
}

.item-box.mine {
  background: var(--opponent-card-bg);
  border-left: 3px solid var(--secondary-color);
}

.item-label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  margin-bottom: 4px;
}

.item-name {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: #1f2937;
  overflow-wrap: break-word;
  word-break: break-word;
}

:global(.dark-mode) .item-name {
  color: #ffffff;
}

.exchange-icon {
  font-size: 1.5rem;
  opacity: 0.6;
  flex-shrink: 0;
  margin: 0 8px;
}

.pc-icon {
  display: block;
}

.mobile-icon {
  display: none;
}

@media (max-width: 480px) {
  .pc-icon {
    display: none;
  }

  .mobile-icon {
    display: block;
    font-size: 0.8rem;
    margin: 0;
  }

  .items-card {
    flex-direction: row;
    gap: 6px;
    padding: 10px;
  }

  .item-box {
    flex: 1;
    padding: 8px;
  }

  .item-label {
    font-size: 0.55rem;
  }

  .item-name {
    font-size: 0.65rem;
  }

  .section-title {
    font-size: 0.8rem;
    margin: 0 0 12px 0;
  }

  .item-content .label {
    font-size: 0.65rem;
  }

  .item-content .value {
    font-size: 0.75rem;
  }

  .detail-label {
    font-size: 0.65rem;
  }

  .detail-value {
    font-size: 0.75rem;
  }
}

/* 조건 섹션 */
.section-title {
  margin: 0 0 16px 0;
  font-size: 0.95rem;
  font-weight: 800;
  color: var(--text-primary);
}

.conditions-section {
  margin-bottom: 24px;
}

.conditions-comparison-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}

.item-comparison {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: stretch;
}

.my-item,
.opponent-item {
  background: var(--hover-bg);
  border-radius: 12px;
  overflow: hidden;
  border: none;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}

/* 일치 상태: 초록색 테두리 & 배경 */
.item-comparison.matched .my-item:not(.no-value),
.item-comparison.matched .opponent-item:not(.no-value) {
  border: 2px solid #10b981;
  background: rgba(16, 185, 129, 0.05);
}

/* 불일치 상태: 테두리 없음 */
.item-comparison.mismatched .my-item,
.item-comparison.mismatched .opponent-item {
  border: 2px solid #ef4444;
  background: rgba(239, 68, 68, 0.05);
}

/* 값이 없는 경우: 빨간 테두리 (미지정 = 불일치) */
.my-item.no-value,
.opponent-item.no-value {
  border: 2px solid #ef4444;
  background: rgba(239, 68, 68, 0.05);
}

.item-header {
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--border-color);
}

.my-item .item-header {
  background: rgba(249, 115, 22, 0.1);
  border-bottom-color: rgba(249, 115, 22, 0.2);
}

.opponent-item .item-header {
  background: rgba(59, 130, 246, 0.1);
  border-bottom-color: rgba(59, 130, 246, 0.2);
}

.item-content {
  padding: 12px;
  display: flex;
  gap: 10px;
  flex: 1;
}

.item-content .icon {
  font-size: 1.3rem;
  flex-shrink: 0;
  margin-top: 2px;
}

.item-content > div {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-content .label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  margin: 0;
}

.item-content .value {
  font-size: 0.9rem;
  font-weight: 700;
  color: #ef4444;
  margin: 0;
  transition: color 0.3s ease;
}

:global(.dark-mode) .item-content .value {
  color: #ffffff;
}

/* 일치 상태 텍스트 */
.item-comparison.matched .item-content .value {
  color: #10b981;
}

:global(.dark-mode) .item-comparison.matched .item-content .value {
  color: #10b981;
}

/* 불일치 상태 텍스트 */
.item-comparison.mismatched .item-content .value {
  color: #ef4444;
}

:global(.dark-mode) .item-comparison.mismatched .item-content .value {
  color: #ef4444;
}

.item-content.no-value .value {
  color: #ef4444 !important;
}



.badge {
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
  padding: 2px 6px;
  border-radius: 3px;
}

.my-item .badge {
  color: var(--secondary-color);
}

.opponent-item .badge {
  color: var(--primary-color);
}

.badge-latest {
  font-size: 0.65rem;
  background: #10b981;
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  text-transform: none;
}

.messages-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: var(--hover-bg);
  border-radius: 12px;
  border-left: 3px solid var(--link-color);
}

.messages-title {
  margin: 0 0 8px 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
}

.message-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.3);
}

.message-row.my-message {
  border-left: 3px solid #f97316;
}

.message-row.opponent-message {
  border-left: 3px solid #3b82f6;
}

.message-row .badge {
  flex-shrink: 0;
  margin-top: 2px;
  font-weight: 600;
  color: var(--text-primary);
}

.message-text {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.4;
  word-break: break-word;
}

:global(.dark-mode) .message-row {
  background: rgba(0, 0, 0, 0.2);
}

/* 협의 진행 중 메시지 */
.negotiation-status-message {
  margin-top: 16px;
  padding: 16px;
  background: rgba(107, 114, 128, 0.05);
  border: 2px dashed var(--border-color);
  border-radius: 12px;
}

.status-content {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.status-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.status-text {
  margin: 0 0 4px 0;
  font-size: 1rem;
  font-weight: 800;
  color: var(--text-primary);
}

.status-desc {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.vs-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

/* 협상 진행 과정 아코디언 */
.negotiation-history-section {
  margin-bottom: 24px;
}

/* 최종 합의 완료 안내문 */
.agreement-notice {
  margin: 16px 0 24px;
  padding: 16px;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  border-radius: 12px;
}

.agreement-notice .notice-content {
  display: flex;
  gap: 12px;
  align-items: center;
}

.agreement-notice .notice-icon {
  font-size: 1.2rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.agreement-notice .notice-text {
  margin: 0;
  color: #78350f;
  font-size: 0.9rem;
  line-height: 1.5;
}

:global(.dark-mode) .agreement-notice {
  background: rgba(120, 53, 15, 0.2);
  border-color: #d97706;
}

:global(.dark-mode) .agreement-notice .notice-text {
  color: #fcd34d;
}

/* 협상 결렬 안내 */
.rejection-notice {
  margin: 16px 0 24px;
  padding: 16px;
  background: #f3f4f6;
  border: 1px solid #d1d5db;
  border-radius: 12px;
}

.rejection-notice .notice-content {
  display: flex;
  gap: 12px;
  align-items: center;
}

.rejection-notice .notice-icon {
  font-size: 1.2rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.rejection-notice .notice-text {
  margin: 0;
  color: #6b7280;
  font-size: 0.9rem;
  line-height: 1.5;
  font-weight: 600;
}

:global(.dark-mode) .rejection-notice {
  background: rgba(75, 85, 99, 0.2);
  border-color: #6b7280;
}

:global(.dark-mode) .rejection-notice .notice-text {
  color: #d1d5db;
}

.cancellation-notice {
  margin: 16px 0 24px;
  padding: 16px;
  background: #fee;
  border: 1px solid #fecaca;
  border-radius: 12px;
}

.cancellation-notice .notice-content {
  display: flex;
  gap: 12px;
  align-items: center;
}

.cancellation-notice .notice-icon {
  font-size: 1.2rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.cancellation-notice .notice-text {
  margin: 0;
  color: #dc2626;
  font-size: 0.9rem;
  line-height: 1.5;
  font-weight: 600;
}

:global(.dark-mode) .cancellation-notice {
  background: rgba(220, 38, 38, 0.15);
  border-color: #ef4444;
}

:global(.dark-mode) .cancellation-notice .notice-text {
  color: #fca5a5;
}

.accordion-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--hover-bg);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.accordion-header:hover {
  background: rgba(59, 130, 246, 0.05);
}

.accordion-toggle {
  font-size: 0.8rem;
  color: var(--text-secondary);
  transition: transform 0.2s;
  display: inline-block;
  width: 16px;
}

.accordion-header .section-title {
  margin: 0;
  font-size: 0.95rem;
}

.accordion-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.history-item {
  padding: 16px;
  background: var(--hover-bg);
  border-radius: 12px;
  border-left: 4px solid var(--primary-color);
  transition: border-color 0.2s;
}

.history-item.is-me {
  border-left-color: var(--secondary-color);
}

.round-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.round-badge {
  background: var(--primary-color);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 800;
}

.actor-badge {
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 0.75rem;
  font-weight: 700;
}

.actor-badge.proposer {
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary-color);
}

.actor-badge.proposer.is-me {
  background: rgba(249, 115, 22, 0.1);
  color: var(--secondary-color);
}

.actor-badge.proposer.is-opponent {
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary-color);
}

.actor-badge.receiver {
  background: rgba(249, 115, 22, 0.1);
  color: var(--secondary-color);
}

.actor-badge.receiver.is-me {
  background: rgba(249, 115, 22, 0.1);
  color: var(--secondary-color);
}

.actor-badge.receiver.is-opponent {
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary-color);
}

.created-time {
  font-size: 0.75rem;
  color: var(--text-secondary);
  margin-left: auto;
}

.history-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  padding: 8px;
  background: var(--card-bg);
  border-radius: 6px;
}

.detail-label {
  font-weight: 700;
  color: var(--text-secondary);
}

.detail-value {
  color: var(--text-primary);
  text-align: right;
  word-break: break-word;
}

/* 거래 완료 섹션 */
.completion-section {
  margin-bottom: 24px;
  padding: 16px;
  background: var(--agreed-card-bg);
  border-radius: 12px;
}

.completion-status {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.status-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  border-left: 3px solid var(--border-color);
}

.status-item.completed {
  border-left-color: var(--success-color);
  background: rgba(16, 185, 129, 0.05);
}

.status-icon {
  font-size: 1.2rem;
  flex-shrink: 0;
}

.status-item > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-primary);
}

.status-time {
  font-size: 0.75rem;
  color: var(--text-secondary);
  margin: 0;
}

.btn-complete {
  width: 100%;
  padding: 12px;
  background: var(--success-color);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-complete:hover {
  background: #059669;
}

/* 메시지 섹션 */
.message-section {
  margin-bottom: 24px;
}

.message-box {
  padding: 16px;
  background: var(--hover-bg);
  border-radius: 12px;
  border-left: 3px solid var(--link-color);
}

.message-box p {
  margin: 0;
  font-size: 0.9rem;
  color: var(--text-primary);
  line-height: 1.6;
}

/* 액션 버튼 */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-secondary {
  background: var(--border-color);
  color: var(--text-primary);
}

.btn-secondary:hover {
  background: var(--hover-bg);
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-danger:hover {
  background: #dc2626;
}

.btn-success {
  background: var(--success-color);
  color: white;
}

.btn-success:hover {
  background: #059669;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 최종 합의 카드 */
.condition-card {
  background: var(--hover-bg);
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid var(--border-color);
}

.condition-card.agreed-card {
  background: var(--agreed-card-bg);
  border-color: var(--success-color);
  padding: 16px;
}

.card-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-header.agreed {
  background: rgba(16, 185, 129, 0.1);
  border-bottom-color: rgba(16, 185, 129, 0.2);
}

.agreed-badge {
  color: var(--success-color);
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
}

.card-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.condition-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.condition-row .icon {
  font-size: 1.2rem;
  flex-shrink: 0;
  margin-top: 2px;
}

.condition-row > div {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.condition-row .label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
}

.condition-row .value {
  font-size: 0.9rem;
  font-weight: 700;
  color: #1f2937;
}

:global(.dark-mode) .condition-row .value {
  color: #ffffff;
}


@media (max-width: 768px) {
  .item-comparison {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .item-content {
    padding: 10px;
  }

  .item-content .icon {
    font-size: 1.1rem;
  }

  .item-content .label {
    font-size: 0.7rem;
  }

  .item-content .value {
    font-size: 0.85rem;
  }

  .items-card {
    flex-direction: row;
    gap: 8px;
  }

  .exchange-icon {
    transform: none;
  }
}

/* 거래 취소 상태 */
.cancelled-notice {
  background: #fee;
  border: 2px solid #ef4444;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  text-align: center;
}

.cancelled-notice p {
  margin: 6px 0;
  color: #dc2626;
  font-weight: 700;
  font-size: 0.95rem;
}

.cancelled-notice .cancelled-reason {
  font-size: 0.85rem;
  font-weight: 600;
}
</style>
