<template>
  <GlobalModal @close="$emit('close')" class="proposal-detail-modal">
    <template #header>
      <div class="detail-header">
        <h3>{{ proposal.receiverPublicId === authStore.user.publicId ? '받은 제안' : '보낸 제안' }}</h3>
        <div class="tabs-container">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            :class="['tab-btn', { active: activeTab === tab.id }]"
            @click="activeTab = tab.id">
            {{ tab.label }}
          </button>
        </div>
      </div>
    </template>

    <template #body>
      <!-- 기본 정보 탭 -->
      <div v-if="activeTab === 'basic'" class="tab-content basic-info">
        <div class="info-section">
          <div class="section-title">거래 물품</div>
          <div class="items-display">
            <div class="item">
              <span class="label">상대 물건</span>
              <p>{{ getOpponentItem(proposal).title }}</p>
            </div>
            <div class="arrow">→</div>
            <div class="item">
              <span class="label">나의 물건</span>
              <p>{{ getMyItem(proposal).title }}</p>
            </div>
          </div>
        </div>

        <div v-if="proposal.message" class="info-section">
          <div class="section-title">제안 메시지</div>
          <p class="message-text">{{ proposal.message }}</p>
        </div>

        <div class="info-section">
          <div class="section-title">거래 상태</div>
          <div class="status-info">
            <span class="status-label">현재 상태:</span>
            <span class="status-value" :style="{ color: getStatusConfig(proposal.status).color }">
              {{ getStatusConfig(proposal.status).label }}
            </span>
          </div>
          <div class="status-info">
            <span class="status-label">제안 일시:</span>
            <span class="status-value">{{ formatDate(proposal.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 조건 협의 탭 -->
      <div v-if="activeTab === 'schedule'" class="tab-content schedule-info">
        <div v-if="getScheduleStatus(proposal) === 'NO_SCHEDULE'" class="empty-state">
          <p>⚙️ 아직 거래 조건이 제시되지 않았습니다.</p>
        </div>

        <div v-else class="conditions-display">
          <div v-if="proposal.proposerScheduleJson" class="condition-block">
            <div class="condition-title">제안자의 조건</div>
            <div class="condition-details" v-if="parsedProposerSchedule">
              <div class="condition-item">
                <span class="label">거래 방식</span>
                <span class="value">{{ { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }[parsedProposerSchedule.method] }}</span>
              </div>
              <div class="condition-item">
                <span class="label">장소</span>
                <span class="value">{{ parsedProposerSchedule.location || '미지정' }}</span>
              </div>
              <div class="condition-item">
                <span class="label">시간</span>
                <span class="value">{{ formatDateTime(parsedProposerSchedule.tradeDateTime) }}</span>
              </div>
            </div>
          </div>

          <div v-if="proposal.receiverScheduleJson && proposal.receiverScheduleJson !== proposal.proposerScheduleJson" class="condition-block">
            <div class="condition-title">수신자의 반박</div>
            <div class="condition-details" v-if="parsedReceiverSchedule">
              <div class="condition-item">
                <span class="label">거래 방식</span>
                <span class="value">{{ { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }[parsedReceiverSchedule.method] }}</span>
              </div>
              <div class="condition-item">
                <span class="label">장소</span>
                <span class="value">{{ parsedReceiverSchedule.location || '미지정' }}</span>
              </div>
              <div class="condition-item">
                <span class="label">시간</span>
                <span class="value">{{ formatDateTime(parsedReceiverSchedule.tradeDateTime) }}</span>
              </div>
            </div>
          </div>

          <div v-if="proposal.agreedScheduleJson" class="condition-block agreed">
            <div class="condition-title">✅ 확정된 조건</div>
            <div class="condition-details" v-if="parsedAgreedSchedule">
              <div class="condition-item">
                <span class="label">거래 방식</span>
                <span class="value">{{ { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }[parsedAgreedSchedule.method] }}</span>
              </div>
              <div class="condition-item">
                <span class="label">장소</span>
                <span class="value">{{ parsedAgreedSchedule.location || '미지정' }}</span>
              </div>
              <div class="condition-item">
                <span class="label">시간</span>
                <span class="value">{{ formatDateTime(parsedAgreedSchedule.tradeDateTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </template>
  </GlobalModal>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import GlobalModal from '@/components/common/GlobalModal.vue'
import { PROPOSAL_STATUS } from '@/constants/postConstants'

const props = defineProps({
  proposal: { type: Object, required: true }
})

const emit = defineEmits(['close', 'success'])

const authStore = useAuthStore()
const uiStore = useUiStore()

const activeTab = ref('basic')

const tabs = [
  { id: 'basic', label: '기본 정보' },
  { id: 'schedule', label: '조건 협의' }
]

const parsedProposerSchedule = computed(() => {
  if (!props.proposal.proposerScheduleJson) return null
  try {
    return JSON.parse(props.proposal.proposerScheduleJson)
  } catch {
    return null
  }
})

const parsedReceiverSchedule = computed(() => {
  if (!props.proposal.receiverScheduleJson) return null
  try {
    return JSON.parse(props.proposal.receiverScheduleJson)
  } catch {
    return null
  }
})

const parsedAgreedSchedule = computed(() => {
  if (!props.proposal.agreedScheduleJson) return null
  try {
    return JSON.parse(props.proposal.agreedScheduleJson)
  } catch {
    return null
  }
})

const getScheduleStatus = (proposal) => {
  if (!proposal.proposerScheduleJson && !proposal.receiverScheduleJson) {
    return 'NO_SCHEDULE'
  }
  if (proposal.agreedScheduleJson) {
    return 'AGREED'
  }
  return 'NEGOTIATING'
}

const getStatusConfig = (status) => PROPOSAL_STATUS[status] || { label: status, color: '#6b7280' }

const getOpponentItem = (proposal) => {
  return proposal.receiverPublicId === authStore.user.publicId ? proposal.offeredPost : proposal.barterPost
}

const getMyItem = (proposal) => {
  return proposal.receiverPublicId === authStore.user.publicId ? proposal.barterPost : proposal.offeredPost
}

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${d.getMonth()+1}.${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

const formatDateTime = (val) => {
  if (!val) return '미지정'
  const d = new Date(val)
  return `${d.getFullYear()}.${d.getMonth()+1}.${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.proposal-detail-modal { }

.detail-header { padding: 0; }
.detail-header h3 { margin: 0; font-size: 1.1rem; font-weight: 800; }

.tabs-container {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  border-bottom: 2px solid var(--border-color);
  overflow-x: auto;
}

.tab-btn {
  padding: 8px 16px;
  border: none;
  background: none;
  color: var(--text-secondary);
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.2s;
  white-space: nowrap;
}

.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active { color: var(--link-color); border-bottom-color: var(--link-color); }

.tab-content { padding: 20px 0; }

/* 기본 정보 탭 */
.basic-info { }
.info-section { margin-bottom: 24px; }
.section-title { font-size: 0.9rem; font-weight: 800; color: var(--text-secondary); margin-bottom: 12px; text-transform: uppercase; }

.items-display {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--hover-bg);
  padding: 16px;
  border-radius: 12px;
}

.item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.item .label { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); }
.item p { font-size: 0.9rem; font-weight: 700; color: var(--text-primary); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.arrow { font-size: 1.2rem; opacity: 0.5; flex-shrink: 0; }

.message-text { margin: 0; font-size: 0.9rem; color: var(--text-primary); line-height: 1.6; }

.status-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}

.status-label { font-size: 0.85rem; color: var(--text-secondary); }
.status-value { font-size: 0.85rem; font-weight: 700; }

/* 조건 협의 탭 */
.schedule-info { }
.conditions-display { display: flex; flex-direction: column; gap: 16px; }

.condition-block {
  background: var(--hover-bg);
  padding: 16px;
  border-radius: 12px;
  border-left: 4px solid var(--link-color);
}

.condition-block.agreed { border-left-color: #10b981; }

.condition-title {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.condition-details { display: flex; flex-direction: column; gap: 8px; }

.condition-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.condition-item .label { font-size: 0.8rem; color: var(--text-secondary); }
.condition-item .value { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); }


@media (max-width: 768px) {
  .items-display { flex-direction: column; gap: 12px; }
  .arrow { transform: rotate(90deg); }
}
</style>
