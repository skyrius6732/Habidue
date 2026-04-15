<template>
  <GlobalModal @close="$emit('close')">
    <template #header>
      <h3>
        <span v-if="mode === 'propose'">⚙️ 거래 조건 제시</span>
        <span v-else-if="mode === 'accept'">✅ 거래 조건 수락</span>
        <span v-else-if="mode === 'counter'">✏️ 거래 조건 변경</span>
      </h3>
    </template>
    
    <template #body>
      <!-- [차단 안내] 상대방이 나를 차단했으면 불가 -->
      <div v-if="proposal.blockedByPartner" class="blocked-notice">
        <p>⛔ 상대방에게 차단되어 물물교환이 불가합니다.</p>
      </div>

      <div class="wizard-container">
        <!-- 단계 표시 (수락 모드는 1단계만) -->
        <div class="wizard-steps">
          <div v-for="i in (mode === 'accept' ? 1 : 4)" :key="i" class="step-dot" :class="{ active: currentStep >= i, current: currentStep === i }">
            {{ mode === 'accept' ? '확인' : i }}
          </div>
        </div>

        <div class="step-content">
          <!-- 수락 모드: 상대 조건 확인 + 자신의 조건 입력 선택 -->
          <div v-if="mode === 'accept' && currentStep === 1" class="step-inner-accept">
            <div class="opponent-condition-box">
              <h4 class="step-title">상대방이 제시한 조건</h4>
              <div v-if="opponentSchedule" class="confirmation-card">
                <div class="conf-item">
                  <span class="conf-label">거래 방식</span>
                  <span class="conf-value">{{ { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }[opponentSchedule.method] }}</span>
                </div>
                <div class="conf-item">
                  <span class="conf-label">거래 장소</span>
                  <span class="conf-value">{{ opponentSchedule.location || '미지정' }}</span>
                </div>
                <div class="conf-item">
                  <span class="conf-label">거래 시간</span>
                  <span class="conf-value">{{ formatDateTime(opponentSchedule.tradeDateTime) }}</span>
                </div>
              </div>
            </div>

            <div class="action-notice">
              <p>💡 상대방 조건을 그대로 수락하거나,</p>
              <p>'다음'을 눌러 당신의 조건을 입력한 후 변경 제안할 수 있습니다.</p>
            </div>
          </div>

          <!-- 수락 모드: 자신의 조건 입력 (Step 2-4) -->
          <template v-if="mode === 'accept' && currentStep >= 2">
            <!-- Step 2: 방식 선택 -->
            <div v-if="currentStep === 2" class="step-inner">
              <h4 class="step-title">자신의 거래 방식을 선택해 주세요</h4>

              <!-- 빠른 선택 -->
              <div v-if="opponentExistingSchedule || myExistingSchedule" class="quick-select-card">
                <div class="quick-select-title">빠른 선택</div>
                <div v-if="opponentExistingSchedule" class="quick-option">
                  <button @click="method = opponentExistingSchedule.method" class="quick-option-btn">
                    ○ 상대방 조건: {{ { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }[opponentExistingSchedule.method] }}
                  </button>
                </div>
                <div v-if="myExistingSchedule" class="quick-option">
                  <button @click="method = myExistingSchedule.method" class="quick-option-btn">
                    ○ 내 기존 조건: {{ { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }[myExistingSchedule.method] }}
                  </button>
                </div>
              </div>

              <!-- 직접 선택 -->
              <div v-if="opponentExistingSchedule || myExistingSchedule" class="direct-select-title">직접 선택</div>

              <div class="options-grid">
                <button class="opt-btn" :class="{ active: method === 'DIRECT' }" @click="method = 'DIRECT'">
                  <span class="opt-icon">🤝</span>
                  <span class="opt-label">직거래</span>
                </button>
                <button class="opt-btn" :class="{ active: method === 'DOORSTEP' }" @click="method = 'DOORSTEP'">
                  <span class="opt-icon">🚪</span>
                  <span class="opt-label">문고리 거래</span>
                </button>
                <button class="opt-btn" :class="{ active: method === 'PARCEL' }" @click="method = 'PARCEL'">
                  <span class="opt-icon">📦</span>
                  <span class="opt-label">택배 교환</span>
                </button>
              </div>
            </div>

            <!-- Step 3: 주소/장소 입력 -->
            <div v-if="currentStep === 3" class="step-inner">
              <div v-if="method === 'DIRECT'">
                <h4 class="step-title">자신의 거래 장소를 입력해 주세요</h4>

                <!-- 빠른 선택 -->
                <div v-if="opponentExistingSchedule || myExistingSchedule" class="quick-select-card">
                  <div class="quick-select-title">빠른 선택</div>
                  <div v-if="opponentExistingSchedule && opponentExistingSchedule.method === 'DIRECT'" class="quick-option">
                    <button @click="location = opponentExistingSchedule.location" class="quick-option-btn">
                      ○ 상대방: {{ opponentExistingSchedule.location }}
                    </button>
                  </div>
                  <div v-if="myExistingSchedule && myExistingSchedule.method === 'DIRECT'" class="quick-option">
                    <button @click="location = myExistingSchedule.location" class="quick-option-btn">
                      ○ 내 기존: {{ myExistingSchedule.location }}
                    </button>
                  </div>
                </div>

                <!-- 직접 입력 -->
                <div v-if="opponentExistingSchedule || myExistingSchedule" class="direct-select-title">또는 직접 입력</div>

                <div class="location-input-group">
                  <input v-model="location" placeholder="예: 강남역 2번 출구" class="wizard-input" />
                </div>
              </div>
              <div v-else>
                <h4 class="step-title">자신의 거래 주소를 입력해 주세요</h4>

                <!-- 상대방이 제시한 발송지 표시 (읽기만 가능) -->
                <div v-if="opponentExistingSchedule && (opponentExistingSchedule.method === 'DOORSTEP' || opponentExistingSchedule.method === 'PARCEL')" class="opponent-info-card">
                  <div class="opponent-info-label">상대방 발송지</div>
                  <div class="opponent-info-value">{{ opponentExistingSchedule.senderAddress }}</div>
                </div>

                <!-- 빠른 선택 -->
                <div v-if="opponentExistingSchedule || myExistingSchedule" class="quick-select-card">
                  <div class="quick-select-title">빠른 선택</div>
                  <div v-if="opponentExistingSchedule && (opponentExistingSchedule.method === 'DOORSTEP' || opponentExistingSchedule.method === 'PARCEL')" class="quick-option">
                    <button @click="[senderAddress = opponentExistingSchedule.receiverAddress, receiverAddress = opponentExistingSchedule.senderAddress]" class="quick-option-btn">
                      ○ 상대방 조건 반영
                    </button>
                  </div>
                  <div v-if="myExistingSchedule && (myExistingSchedule.method === 'DOORSTEP' || myExistingSchedule.method === 'PARCEL')" class="quick-option">
                    <button @click="[senderAddress = myExistingSchedule.senderAddress, receiverAddress = myExistingSchedule.receiverAddress]" class="quick-option-btn">
                      ○ 내 기존: {{ myExistingSchedule.senderAddress }}
                    </button>
                  </div>
                </div>

                <!-- 직접 입력 -->
                <div v-if="opponentExistingSchedule || myExistingSchedule" class="direct-select-title">또는 직접 입력</div>

                <div class="address-input-group">
                  <div class="address-field">
                    <label class="address-label">📍 내 주소</label>
                    <input v-model="senderAddress" placeholder="예: 서울시 강남구" class="wizard-input" />
                  </div>
                  <div class="address-field">
                    <label class="address-label">📍 상대방 주소</label>
                    <input v-model="receiverAddress" placeholder="예: 부산시 해운대구" class="wizard-input" disabled />
                  </div>
                </div>
              </div>
            </div>

            <!-- Step 4: 시간 입력 -->
            <div v-if="currentStep === 4" class="step-inner">
              <h4 class="step-title">자신의 거래 시간을 정해 주세요</h4>

              <!-- 빠른 선택 -->
              <div v-if="opponentExistingSchedule || myExistingSchedule" class="quick-select-card">
                <div class="quick-select-title">빠른 선택</div>
                <div v-if="opponentExistingSchedule" class="quick-option">
                  <button @click="tradeDateTime = opponentExistingSchedule.tradeDateTime" class="quick-option-btn">
                    ○ 상대방: {{ formatDateTime(opponentExistingSchedule.tradeDateTime) }}
                  </button>
                </div>
                <div v-if="myExistingSchedule" class="quick-option">
                  <button @click="tradeDateTime = myExistingSchedule.tradeDateTime" class="quick-option-btn">
                    ○ 내 기존: {{ formatDateTime(myExistingSchedule.tradeDateTime) }}
                  </button>
                </div>
              </div>

              <!-- 또는 직접 입력 -->
              <div v-if="opponentExistingSchedule || myExistingSchedule" class="direct-select-title">또는 직접 입력</div>

              <input type="datetime-local" v-model="tradeDateTime" class="wizard-input" />
            </div>

            <!-- Step 5: 최종 확인 -->
            <div v-if="currentStep === 5" class="step-inner">
              <h4 class="step-title">당신의 조건을 확인해 주세요</h4>
              <div class="confirmation-card">
                <div class="conf-item">
                  <span class="conf-label">거래 방식</span>
                  <span class="conf-value">{{ methodLabel }}</span>
                </div>
                <template v-if="method === 'DIRECT'">
                  <div class="conf-item">
                    <span class="conf-label">거래 장소</span>
                    <span class="conf-value">{{ location || '미지정' }}</span>
                  </div>
                </template>
                <template v-else>
                  <div class="conf-item">
                    <span class="conf-label">나의 주소</span>
                    <span class="conf-value">{{ senderAddress || '미지정' }}</span>
                  </div>
                  <div class="conf-item">
                    <span class="conf-label">상대방 주소</span>
                    <span class="conf-value">{{ receiverAddress || '미지정' }}</span>
                  </div>
                </template>
                <div class="conf-item">
                  <span class="conf-label">거래 시간</span>
                  <span class="conf-value">{{ formatDateTime(tradeDateTime) }}</span>
                </div>
              </div>
              <p class="conf-notice">💡 이 조건으로 변경 제안합니다.</p>
            </div>
          </template>

          <!-- Step 1: 방식 선택 (제안/반박 모드) -->
          <div v-if="mode !== 'accept' && currentStep === 1" class="step-inner">
            <h4 class="step-title">거래 방식을 선택해 주세요</h4>

            <!-- 빠른 선택 -->
            <div v-if="opponentExistingSchedule || myExistingSchedule" class="quick-select-card">
              <div class="quick-select-title">빠른 선택</div>
              <div v-if="opponentExistingSchedule" class="quick-option">
                <button @click="method = opponentExistingSchedule.method" class="quick-option-btn">
                  ○ 상대방 조건: {{ { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }[opponentExistingSchedule.method] }}
                </button>
              </div>
              <div v-if="myExistingSchedule" class="quick-option">
                <button @click="method = myExistingSchedule.method" class="quick-option-btn">
                  ○ 내 기존 조건: {{ { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }[myExistingSchedule.method] }}
                </button>
              </div>
            </div>

            <!-- 직접 선택 -->
            <div v-if="opponentExistingSchedule || myExistingSchedule" class="direct-select-title">직접 선택</div>

            <div class="options-grid">
              <button class="opt-btn" :class="{ active: method === 'DIRECT' }" @click="method = 'DIRECT'">
                <span class="opt-icon">🤝</span>
                <span class="opt-label">직거래</span>
              </button>
              <button class="opt-btn" :class="{ active: method === 'DOORSTEP' }" @click="method = 'DOORSTEP'">
                <span class="opt-icon">🚪</span>
                <span class="opt-label">문고리 거래</span>
              </button>
              <button class="opt-btn" :class="{ active: method === 'PARCEL' }" @click="method = 'PARCEL'">
                <span class="opt-icon">📦</span>
                <span class="opt-label">택배 교환</span>
              </button>
            </div>
          </div>

          <!-- Step 2: 장소/주소 선택 (제안/반박 모드) -->
          <div v-if="mode !== 'accept' && currentStep === 2" class="step-inner">
            <!-- DIRECT: 공통 만남 장소 -->
            <div v-if="method === 'DIRECT'">
              <h4 class="step-title">거래 장소를 입력해 주세요</h4>

              <!-- 빠른 선택 -->
              <div v-if="opponentExistingSchedule || myExistingSchedule" class="quick-select-card">
                <div class="quick-select-title">빠른 선택</div>
                <div v-if="opponentExistingSchedule && opponentExistingSchedule.method === 'DIRECT'" class="quick-option">
                  <button @click="location = opponentExistingSchedule.location" class="quick-option-btn">
                    ○ 상대방: {{ opponentExistingSchedule.location }}
                  </button>
                </div>
                <div v-if="myExistingSchedule && myExistingSchedule.method === 'DIRECT'" class="quick-option">
                  <button @click="location = myExistingSchedule.location" class="quick-option-btn">
                    ○ 내 기존: {{ myExistingSchedule.location }}
                  </button>
                </div>
              </div>

              <!-- 또는 직접 입력 -->
              <div v-if="opponentExistingSchedule || myExistingSchedule" class="direct-select-title">또는 직접 입력</div>

              <div class="location-input-group">
                <input v-model="location" placeholder="예: 강남역 2번 출구, OO아파트 101동 앞" class="wizard-input" />
                <div class="quick-locations">
                  <button v-for="loc in ['집 앞', '지하철역', '편의점']" :key="loc" @click="location = loc" class="quick-loc-btn">
                    {{ loc }}
                  </button>
                </div>
              </div>
            </div>

            <!-- DOORSTEP/PARCEL: 발송자/수신자 주소 -->
            <div v-else>
              <h4 class="step-title">거래 주소를 입력해 주세요</h4>

              <!-- 상대방이 제시한 발송지 표시 (읽기만 가능) -->
              <div v-if="opponentExistingSchedule && (opponentExistingSchedule.method === 'DOORSTEP' || opponentExistingSchedule.method === 'PARCEL')" class="opponent-info-card">
                <div class="opponent-info-label">상대방 발송지</div>
                <div class="opponent-info-value">{{ opponentExistingSchedule.senderAddress }}</div>
              </div>

              <!-- 빠른 선택 -->
              <div v-if="opponentExistingSchedule || myExistingSchedule" class="quick-select-card">
                <div class="quick-select-title">빠른 선택</div>
                <div v-if="opponentExistingSchedule && (opponentExistingSchedule.method === 'DOORSTEP' || opponentExistingSchedule.method === 'PARCEL')" class="quick-option">
                  <button @click="[senderAddress = opponentExistingSchedule.receiverAddress, receiverAddress = opponentExistingSchedule.senderAddress]" class="quick-option-btn">
                    ○ 상대방 조건 반영
                  </button>
                </div>
                <div v-if="myExistingSchedule && (myExistingSchedule.method === 'DOORSTEP' || myExistingSchedule.method === 'PARCEL')" class="quick-option">
                  <button @click="[senderAddress = myExistingSchedule.senderAddress, receiverAddress = myExistingSchedule.receiverAddress]" class="quick-option-btn">
                    ○ 내 기존: {{ myExistingSchedule.senderAddress }}
                  </button>
                </div>
              </div>

              <!-- 또는 직접 입력 -->
              <div v-if="opponentExistingSchedule || myExistingSchedule" class="direct-select-title">또는 직접 입력</div>

              <div class="address-input-group">
                <div class="address-field">
                  <label class="address-label">📍 내 주소</label>
                  <input v-model="senderAddress" placeholder="예: 서울시 강남구 강남대로" class="wizard-input" />
                </div>
                <div class="address-field">
                  <label class="address-label">📍 상대방 주소</label>
                  <input v-model="receiverAddress" placeholder="예: 부산시 해운대구 센텀시티" class="wizard-input" disabled />
                </div>
              </div>
            </div>
          </div>

          <!-- Step 3: 시간 선택 (제안/반박 모드) -->
          <div v-if="mode !== 'accept' && currentStep === 3" class="step-inner">
            <h4 class="step-title">거래 시간을 정해 주세요</h4>

            <!-- 빠른 선택 -->
            <div v-if="opponentExistingSchedule || myExistingSchedule" class="quick-select-card">
              <div class="quick-select-title">빠른 선택</div>
              <div v-if="opponentExistingSchedule" class="quick-option">
                <button @click="tradeDateTime = opponentExistingSchedule.tradeDateTime" class="quick-option-btn">
                  ○ 상대방: {{ formatDateTime(opponentExistingSchedule.tradeDateTime) }}
                </button>
              </div>
              <div v-if="myExistingSchedule" class="quick-option">
                <button @click="tradeDateTime = myExistingSchedule.tradeDateTime" class="quick-option-btn">
                  ○ 내 기존: {{ formatDateTime(myExistingSchedule.tradeDateTime) }}
                </button>
              </div>
            </div>

            <!-- 또는 직접 입력 -->
            <div v-if="opponentExistingSchedule || myExistingSchedule" class="direct-select-title">또는 직접 입력</div>

            <input type="datetime-local" v-model="tradeDateTime" class="wizard-input" />
          </div>

          <!-- Step 4: 최종 확인 (제안/반박 모드) -->
          <div v-if="mode !== 'accept' && currentStep === 4" class="step-inner">
            <h4 class="step-title">설정된 조건을 확인해 주세요</h4>
            <div class="confirmation-card">
              <div class="conf-item">
                <span class="conf-label">거래 방식</span>
                <span class="conf-value">{{ methodLabel }}</span>
              </div>
              <!-- DIRECT: 장소 1개 -->
              <template v-if="method === 'DIRECT'">
                <div class="conf-item">
                  <span class="conf-label">거래 장소</span>
                  <span class="conf-value">{{ location || '미지정' }}</span>
                </div>
              </template>
              <!-- DOORSTEP/PARCEL: 주소 2개 -->
              <template v-else>
                <div class="conf-item">
                  <span class="conf-label">나의 주소</span>
                  <span class="conf-value">{{ senderAddress || '미지정' }}</span>
                </div>
                <div class="conf-item">
                  <span class="conf-label">상대방 주소</span>
                  <span class="conf-value">{{ receiverAddress || '미지정' }}</span>
                </div>
              </template>
              <div class="conf-item">
                <span class="conf-label">거래 시간</span>
                <span class="conf-value">{{ formatDateTime(tradeDateTime) }}</span>
              </div>
            </div>
            <p class="conf-notice">💡 위 조건으로 상대방과 협의를 진행합니다.</p>
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="wizard-footer">
        <!-- 수락 모드 -->
        <template v-if="mode === 'accept'">
          <!-- Step 1: 상대 조건 확인 시 3개 버튼 -->
          <template v-if="currentStep === 1">
            <button class="btn-action-modal accept-modal" :disabled="submitting || proposal.blockedByPartner" @click="submitAccept">
              ✅ 수락하기
            </button>
            <button class="btn-action-modal counter-modal" :disabled="submitting || proposal.blockedByPartner" @click="currentStep = 2">
              ✏️ 조건 변경
            </button>
            <button class="btn-action-modal reject-modal" :disabled="submitting || proposal.blockedByPartner" @click="submitReject">
              ❌ 거절
            </button>
          </template>

          <!-- Step 2-5: 자신의 조건 입력 시 이전/다음/제시 버튼 -->
          <template v-else>
            <button v-if="currentStep > 2" class="btn-prev" :disabled="proposal.blockedByPartner" @click="currentStep--">이전</button>
            <button v-if="currentStep < 5" class="btn-next" :disabled="!isStepValid || proposal.blockedByPartner" @click="currentStep++">다음</button>
            <button v-if="currentStep === 5" class="btn-submit" :disabled="submitting || proposal.blockedByPartner" @click="submitCounter">
              {{ submitting ? '처리 중...' : '✏️ 조건 변경' }}
            </button>
          </template>
        </template>

        <!-- 제안/반박 모드 -->
        <template v-else>
          <button v-if="currentStep > 1" class="btn-prev" :disabled="proposal.blockedByPartner" @click="currentStep--">이전</button>
          <button v-if="currentStep < 4" class="btn-next" :disabled="!isStepValid || proposal.blockedByPartner" @click="currentStep++">다음</button>
          <button v-if="currentStep === 4" class="btn-submit" :disabled="submitting || proposal.blockedByPartner" @click="submitSchedule">
            {{ submitting ? '처리 중...' : (mode === 'propose' ? '⚙️ 제안하기' : '✏️ 변경하기') }}
          </button>
        </template>
      </div>
    </template>
  </GlobalModal>
</template>

<script setup>
import { ref, computed } from 'vue'
import GlobalModal from '@/components/common/GlobalModal.vue'
import { useUiStore } from '@/stores/ui'
import axios from '@/plugins/axios'

const props = defineProps({
  proposal: { type: Object, required: true },
  mode: { type: String, default: 'propose' }, // 'propose', 'accept', 'counter'
  myExistingSchedule: { type: Object, default: null },
  opponentExistingSchedule: { type: Object, default: null }
})

const uiStore = useUiStore()

const emit = defineEmits(['close', 'success'])

const currentStep = ref(1)
const method = ref(props.proposal.schedule?.method || 'DIRECT')
const location = ref(props.proposal.schedule?.location || '')
const senderAddress = ref(props.proposal.schedule?.senderAddress || '')
const receiverAddress = ref(props.proposal.schedule?.receiverAddress || '')
const tradeDateTime = ref(props.proposal.schedule?.tradeDateTime || '')
const submitting = ref(false)

// 모드별 상대 조건 파싱
const opponentSchedule = computed(() => {
  if (!props.proposal.proposerScheduleJson && !props.proposal.receiverScheduleJson) return null
  try {
    if (props.mode === 'accept' || props.mode === 'counter') {
      const json = props.proposal.lastScheduleSetBy === 'PROPOSER'
        ? props.proposal.proposerScheduleJson
        : props.proposal.receiverScheduleJson
      return JSON.parse(json)
    }
  } catch (e) {
  }
  return null
})

const methodLabel = computed(() => {
  const map = { DIRECT: '직거래', DOORSTEP: '문고리 거래', PARCEL: '택배 교환' }
  return map[method.value]
})

const isStepValid = computed(() => {
  // accept 모드 Step 2-5 유효성 검사
  if (props.mode === 'accept') {
    if (currentStep.value === 2) return !!method.value
    if (currentStep.value === 3) {
      if (method.value === 'DIRECT') return !!location.value.trim()
      return !!senderAddress.value.trim()
    }
    if (currentStep.value === 4) return !!tradeDateTime.value
    if (currentStep.value === 5) return true
    return true
  }

  // propose/counter 모드 유효성 검사
  if (currentStep.value === 1) return !!method.value
  if (currentStep.value === 2) {
    if (method.value === 'DIRECT') return !!location.value.trim()
    return !!senderAddress.value.trim()
  }
  if (currentStep.value === 3) return !!tradeDateTime.value
  return true
})

const formatDateTime = (val) => {
  if (!val) return '미지정'
  const d = new Date(val)
  return `${d.getFullYear()}.${d.getMonth()+1}.${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

const submitSchedule = async () => {
  submitting.value = true
  try {
    let endpoint = `/api/barter/proposals/${props.proposal.id}/schedule`
    const payload = {
      method: method.value,
      location: method.value === 'DIRECT' ? location.value : null,
      senderAddress: method.value !== 'DIRECT' ? senderAddress.value : null,
      receiverAddress: method.value !== 'DIRECT' ? receiverAddress.value : null,
      tradeDateTime: tradeDateTime.value
    }

    if (props.mode === 'propose') {
      endpoint += '/propose'
    } else if (props.mode === 'counter') {
      endpoint += '/counter'
    }

    const response = await axios.post(endpoint, payload)
    emit('success')
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '거래 조건 처리에 실패했습니다.', '오류')
  } finally {
    submitting.value = false
  }
}

// [accept 모드 전용 핸들러들]
const submitAccept = async () => {
  submitting.value = true
  try {
    const response = await axios.post(`/api/barter/proposals/${props.proposal.id}/schedule/accept`)
    emit('success')
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '수락 처리에 실패했습니다.', '오류')
  } finally {
    submitting.value = false
  }
}

const submitReject = async () => {
  submitting.value = true
  try {
    const response = await axios.post(`/api/barter/proposals/${props.proposal.id}/respond`, null, {
      params: { status: 'REJECTED' }
    })
    emit('success')
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '거절 처리에 실패했습니다.', '오류')
  } finally {
    submitting.value = false
  }
}

const submitCounter = async () => {
  // 조건 반박 - Step 5 도달 시 모든 필드가 유효함
  submitting.value = true
  try {
    const payload = {
      method: method.value,
      location: method.value === 'DIRECT' ? location.value : null,
      senderAddress: method.value !== 'DIRECT' ? senderAddress.value : null,
      receiverAddress: method.value !== 'DIRECT' ? receiverAddress.value : null,
      tradeDateTime: tradeDateTime.value
    }

    const response = await axios.post(`/api/barter/proposals/${props.proposal.id}/schedule/counter`, payload)
    emit('success')
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '조건 반박에 실패했습니다.', '오류')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.blocked-notice { background: #fee; border-left: 4px solid #ef4444; padding: 16px; border-radius: 8px; margin-bottom: 20px; }
.blocked-notice p { margin: 0; color: #dc2626; font-weight: 700; font-size: 0.95rem; }

.wizard-container { padding: 10px 0; width: 100%; overflow: hidden; }
.wizard-steps { display: flex; justify-content: center; gap: 12px; margin-bottom: 30px; }
.step-content { overflow: hidden; width: 100%; }
.step-dot { width: 30px; height: 30px; border-radius: 50%; background: var(--divider-color); color: var(--text-muted); display: flex; align-items: center; justify-content: center; font-size: 0.85rem; font-weight: 800; transition: all 0.3s; }
.step-dot.active { background: var(--link-color); color: white; }
.step-dot.current { box-shadow: 0 0 0 4px rgba(0, 149, 246, 0.2); transform: scale(1.1); }

.step-inner { display: flex; flex-direction: column; align-items: center; text-align: center; min-height: 200px; width: 100%; }
.step-title { font-size: 1.1rem; font-weight: 800; margin-bottom: 24px; color: var(--text-primary); }

.options-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; width: 100%; }
.opt-btn { 
  display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 20px 10px;
  border: 2px solid var(--border-color); border-radius: 16px; background: var(--card-bg); cursor: pointer; transition: all 0.2s;
}
.opt-btn:hover { border-color: var(--link-color); transform: translateY(-2px); }
.opt-btn.active { border-color: var(--link-color); background: rgba(0, 149, 246, 0.05); }
.opt-icon { font-size: 1.5rem; }
.opt-label { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); }

.location-input-group { width: 100%; }
.address-input-group { width: 100%; display: flex; flex-direction: column; gap: 16px; }
.address-field { display: flex; flex-direction: column; gap: 8px; }
.address-label { font-size: 0.9rem; font-weight: 700; color: var(--text-primary); }
.wizard-input { width: 100%; padding: 14px; border-radius: 12px; border: 1.5px solid var(--border-color); background: var(--hover-bg); font-size: 1rem; color: var(--text-primary); outline: none; }
.wizard-input:focus { border-color: var(--link-color); }

.quick-locations { display: flex; gap: 8px; margin-top: 12px; justify-content: center; }
.quick-loc-btn { padding: 6px 12px; border-radius: 20px; border: 1px solid var(--border-color); background: var(--card-bg); font-size: 0.8rem; cursor: pointer; }

.confirmation-card { width: 100%; background: var(--hover-bg); padding: 20px; border-radius: 16px; display: flex; flex-direction: column; gap: 16px; text-align: left; }
.conf-item { display: flex; justify-content: space-between; align-items: center; }
.conf-label { font-size: 0.85rem; color: var(--text-secondary); }
.conf-value { font-size: 0.95rem; font-weight: 800; color: var(--text-primary); }
.conf-notice { font-size: 0.8rem; color: var(--text-muted); margin-top: 16px; }

/* 상대방 정보 표시 */
.opponent-info-card { width: 100%; background: rgba(107, 114, 128, 0.1); padding: 12px 16px; border-radius: 8px; margin-bottom: 16px; border: 1px solid rgba(107, 114, 128, 0.2); }
.opponent-info-label { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); margin-bottom: 6px; }
.opponent-info-value { font-size: 0.9rem; color: var(--text-primary); font-weight: 600; }

/* 빠른 선택 UI */
.quick-select-card { width: 100%; background: rgba(16, 185, 129, 0.1); padding: 16px; border-radius: 12px; margin-bottom: 20px; border: 1px solid rgba(16, 185, 129, 0.3); }
.quick-select-title { font-size: 0.85rem; font-weight: 800; color: #10b981; margin-bottom: 12px; }
.quick-option { display: flex; align-items: center; margin-bottom: 8px; }
.quick-option:last-child { margin-bottom: 0; }
.quick-option-btn { width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid rgba(16, 185, 129, 0.3); background: white; text-align: left; font-size: 0.85rem; color: #000; cursor: pointer; transition: all 0.2s; }
.quick-option-btn:hover { background: #10b981; border-color: #10b981; color: white; }
.direct-select-title { font-size: 0.8rem; font-weight: 700; color: var(--text-secondary); margin-top: 16px; margin-bottom: 12px; text-align: center; }

/* accept 모드 스타일 */
.step-inner-accept { display: flex; flex-direction: column; gap: 20px; width: 100%; }
.opponent-condition-box { }
.action-notice { padding: 12px 16px; background: rgba(0, 149, 246, 0.1); border-radius: 12px; }
.action-notice p { font-size: 0.85rem; color: var(--text-primary); margin: 4px 0; }

.btn-action-modal { padding: 12px 24px; border-radius: 10px; border: none; font-weight: 800; cursor: pointer; flex: 1; }
.btn-action-modal:disabled { opacity: 0.5; cursor: not-allowed; }
.accept-modal { background: #10b981; color: white; }
.counter-modal { background: #8b5cf6; color: white; }
.reject-modal { color: #ef4444; border: 1px solid #ef4444; background: white; }

.wizard-footer { display: flex; gap: 12px; width: 100%; justify-content: center; margin-top: 20px; }
.btn-prev { padding: 12px 24px; border-radius: 10px; border: 1px solid var(--border-color); background: none; font-weight: 700; cursor: pointer; color: var(--text-primary); }
.btn-next, .btn-submit { padding: 12px 32px; border-radius: 10px; border: none; background: var(--link-color); color: white; font-weight: 800; cursor: pointer; flex: 1; }
.btn-next:disabled, .btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 768px) {
  .step-title { font-size: 0.95rem; margin-bottom: 16px; }
  .wizard-input {
    padding: 8px 10px;
    font-size: 0.85rem;
    box-sizing: border-box;
    max-width: 100%;
  }

  /* datetime-local input 모바일 최적화 - 네이티브 달력 팝업이 화면을 벗어나지 않도록 */
  input[type="datetime-local"] {
    font-size: 0.75rem !important;
    padding: 6px 8px !important;
    line-height: 1.3 !important;
    width: 100%;
    box-sizing: border-box;
    max-width: 100%;
  }

  .step-inner {
    min-height: 150px;
    padding: 0 8px;
  }
  .quick-select-card { padding: 12px; margin-bottom: 16px; }
  .direct-select-title { margin-top: 12px; margin-bottom: 10px; }
  .location-input-group { padding: 0 8px; }
  .address-input-group { padding: 0 8px; }
}
</style>
