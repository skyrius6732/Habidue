<template>
  <Transition name="fade">
    <div class="modal-overlay" @click="$emit('close')">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>🤝 교환 제안하기</h3>
          <button class="btn-close" @click="$emit('close')">✕</button>
        </div>
        
        <div class="modal-body">
          <div class="target-item-preview">
            <span class="preview-label">제안 대상:</span>
            <span class="preview-title">{{ barterPost.title }}</span>
          </div>

          <div class="my-items-section">
            <h4 class="section-title">내 물건 선택 <span class="section-required">(필수)</span></h4>
            <div v-if="loadingMyItems" class="loading-items">내 물건 불러오는 중...</div>
            <div v-else-if="myItems.length > 0" class="items-grid scroll-y">
              <div v-for="item in myItems" :key="item.id" 
                   class="my-item-card" :class="{ active: selectedItemId === item.id }"
                   @click="selectedItemId = item.id">
                <img v-if="item.imageUrls && item.imageUrls.length > 0" :src="item.imageUrls[0]" class="item-thumb" />
                <div v-else class="item-thumb-placeholder">📦</div>
                <div class="item-info">
                  <p class="item-title">{{ item.title }}</p>
                  <span class="item-status">{{ getStatusLabel(item.barterStatus) }}</span>
                </div>
                <div class="check-mark" v-if="selectedItemId === item.id">✓</div>
              </div>
            </div>
            <div v-else class="empty-items">
              <p>등록된 내 물건이 없습니다.</p>
              <button class="btn-go-write" @click="goToWrite">물건 등록하러 가기</button>
            </div>
          </div>

          <div class="message-section">
            <h4 class="section-title">메시지 <span class="section-required">(선택)</span></h4>
            <textarea v-model="message" maxlength="100" placeholder="상태 설명이나 희망 거래 방식을 적어주세요. (최대 100자)" class="proposal-textarea"></textarea>
            <span class="char-count">{{ message.length }}/100</span>
          </div>

          <!-- [차단 안내] 상대방이 나를 차단했으면 불가 -->
          <div v-if="isPartnerBlocked" class="blocked-notice">
            <p>⚠️ 상대방에게 차단되어 물물교환이 불가합니다.</p>
          </div>

          <div class="karma-notice">
            <p>💡 제안 시 <strong>신뢰점수 0.1P</strong>가 소모됩니다.</p>
            <p>신중하게 제안해 주세요!</p>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="$emit('close')">취소</button>
          <button class="btn-submit" :disabled="!selectedItemId || submitting || isPartnerBlocked" @click="submitProposal">
            {{ submitting ? '전송 중...' : '제안 보내기' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '@/stores/ui'
import axios from '@/plugins/axios'
import { BARTER_STATUS } from '@/constants/postConstants'

const props = defineProps({
  barterPost: { type: Object, required: true }
})

const emit = defineEmits(['close', 'success'])
const router = useRouter()
const uiStore = useUiStore()

const myItems = ref([])
const loadingMyItems = ref(true)
const selectedItemId = ref(null)
const message = ref('')
const submitting = ref(false)

const getStatusLabel = (status) => BARTER_STATUS[status]?.label || status

const isPartnerBlocked = computed(() => props.barterPost.blockedByPartner || false)

const fetchMyItems = async () => {
  try {
    const res = await axios.get('/api/posts/my', {
      params: { type: 'BARTER', size: 100 }
    })
    myItems.value = res.data.data.content.filter(item =>
      item.id !== props.barterPost.id && item.barterStatus === 'TRADING'
    )
  } catch (e) {
    console.error('내 물건 로드 실패:', e)
  } finally {
    loadingMyItems.value = false
  }
}

const submitProposal = async () => {
  if (!selectedItemId.value) return
  submitting.value = true
  try {
    await axios.post('/api/barter/proposals', {
      barterPostId: props.barterPost.id,
      offeredPostId: selectedItemId.value,
      message: message.value
    })
    emit('success')
  } catch (e) {
    await uiStore.showAlert(e.response?.data?.message || '제안 보내기에 실패했습니다.', '오류')
  } finally {
    submitting.value = false
  }
}

const goToWrite = () => {
  router.push({ path: '/board/write', query: { menu: 'BARTER' } })
  emit('close')
}

onMounted(fetchMyItems)
</script>

<style scoped>
.modal-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.6); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center; z-index: 3000;
}

.modal-content {
  background: var(--card-bg); width: 90%; max-width: 500px;
  border-radius: 24px; overflow: hidden; display: flex; flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}

.modal-header {
  padding: 20px 24px; border-bottom: 1px solid var(--divider-color);
  display: flex; justify-content: space-between; align-items: center;
}

.modal-header h3 { margin: 0; font-size: 1.2rem; font-weight: 800; }
.btn-close { background: none; border: none; font-size: 1.2rem; color: var(--text-secondary); cursor: pointer; }

.modal-body { padding: 24px; display: flex; flex-direction: column; gap: 24px; max-height: 70vh; overflow-y: auto; }

.target-item-preview {
  background: var(--hover-bg); padding: 12px 16px; border-radius: 12px;
  display: flex; gap: 10px; align-items: center;
}
.preview-label { font-size: 0.85rem; color: var(--text-secondary); }
.preview-title { font-size: 0.9rem; font-weight: 700; color: var(--text-primary); }

.section-title { font-size: 0.95rem; font-weight: 800; margin: 0 0 12px; color: var(--text-primary); }
.section-required { font-size: 0.8rem; font-weight: 500; color: var(--text-secondary); }

.my-items-section { max-height: 280px; display: flex; flex-direction: column; }
.items-grid { display: flex; flex-direction: column; gap: 10px; max-height: 220px; overflow-y: auto; }
.my-item-card {
  display: flex; align-items: center; gap: 12px; padding: 10px;
  border: 1.5px solid var(--border-color); border-radius: 12px; cursor: pointer;
  transition: all 0.2s; position: relative;
}
.my-item-card:hover { border-color: var(--link-color); background: var(--hover-bg); }
.my-item-card.active { border-color: var(--link-color); background: rgba(0, 149, 246, 0.05); }

.item-thumb { width: 50px; height: 50px; border-radius: 8px; object-fit: cover; }
.item-thumb-placeholder { width: 50px; height: 50px; border-radius: 8px; background: var(--divider-color); display: flex; align-items: center; justify-content: center; font-size: 1.2rem; }

.item-info { flex: 1; }
.item-title { font-size: 0.85rem; font-weight: 700; margin: 0 0 4px; color: var(--text-primary); }
.item-status { font-size: 0.75rem; color: var(--link-color); font-weight: 600; }

.check-mark { position: absolute; right: 16px; color: var(--link-color); font-weight: 900; font-size: 1.2rem; }

.proposal-textarea {
  width: 100%; height: 100px; padding: 12px; border-radius: 12px;
  border: 1px solid var(--border-color); background: var(--hover-bg);
  color: var(--text-primary); font-size: 0.9rem; resize: none; outline: none;
}
.proposal-textarea:focus { border-color: var(--link-color); }
.char-count { font-size: 0.75rem; color: var(--text-secondary); text-align: right; display: block; margin-top: 4px; }

.blocked-notice { background: #fee; border-left: 4px solid #ef4444; padding: 12px 16px; border-radius: 8px; }
.blocked-notice p { margin: 0; color: #dc2626; font-weight: 700; font-size: 0.9rem; }

.karma-notice {
  background: rgba(245, 158, 11, 0.05); border: 1px dashed #f59e0b;
  padding: 12px 16px; border-radius: 12px; text-align: center;
}
.karma-notice p { margin: 0; font-size: 0.8rem; color: #d97706; line-height: 1.5; }

.modal-footer {
  padding: 20px 24px; border-top: 1px solid var(--divider-color);
  display: flex; gap: 12px;
}
.btn-cancel { flex: 1; padding: 14px; border-radius: 12px; border: 1px solid var(--border-color); background: none; color: var(--text-secondary); font-weight: 700; cursor: pointer; }
.btn-submit { flex: 2; padding: 14px; border-radius: 12px; border: none; background: var(--link-color); color: white; font-weight: 800; cursor: pointer; }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }

.empty-items { text-align: center; padding: 30px 0; }
.empty-items p { font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 12px; }
.btn-go-write { background: none; border: 1px solid var(--link-color); color: var(--link-color); padding: 8px 16px; border-radius: 8px; font-size: 0.85rem; font-weight: 700; cursor: pointer; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
