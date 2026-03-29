<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessageStore } from '@/stores/message'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const props = defineProps({
  show: Boolean,
  receiverId: [Number, String],
  receiverNickname: String
})

const emit = defineEmits(['close', 'success'])
const messageStore = useMessageStore()
const authStore = useAuthStore()
const uiStore = useUiStore()

const content = ref('')
const selectedFiles = ref([])
const isSending = ref(false)
const errorMessage = ref('')

const displayKarma = computed(() => {
  const points = authStore.user?.karmaPoint || 1000
  return (points / 10).toFixed(1)
})

const isKarmaLow = computed(() => {
  const points = authStore.user?.karmaPoint || 1000
  return points <= 800
})

onMounted(async () => {
  // [시니어 조치] 모달이 열릴 때마다 최신 신뢰 점수와 발송 상태를 동기화
  try {
    await Promise.all([
      authStore.fetchUserProfile(),
      messageStore.fetchDailyStatus()
    ]);
  } catch (e) {
    console.error('사용자 정보 동기화 실패', e);
  }
})

const handleFileChange = async (e) => {
  const files = Array.from(e.target.files)
  // 최대 5개 파일 제한
  if (selectedFiles.value.length + files.length > 5) {
    await uiStore.showAlert('파일은 최대 5개까지만 첨부할 수 있습니다.', '알림')
    return
  }
  selectedFiles.value = [...selectedFiles.value, ...files]
}

const removeFile = (index) => {
  selectedFiles.value.splice(index, 1)
}

const getFilePreview = (file) => {
  if (file.type.startsWith('image/')) {
    return URL.createObjectURL(file)
  }
  return null
}

const handleSend = async () => {
  if (!content.value.trim() && selectedFiles.value.length === 0) return
  
  // [시니어 조치] 신뢰 점수 부족 시 발송 차단 (구체적인 점수 명시)
  if (isKarmaLow.value) {
    await uiStore.showAlert('⚠️ 신뢰 점수(Karma)가 80.0점 미만이라 쪽지 발송이 제한되었습니다.\n건전한 활동을 통해 80.0점 이상으로 회복해 주세요.', '발송 제한')
    return
  }

  if (!props.receiverId) {
    errorMessage.value = '수신자 정보를 찾을 수 없습니다. 다시 시도해 주세요.'
    return
  }

  isSending.value = true
  errorMessage.value = ''
  
  // 백엔드에서 String으로 받으므로 그대로 전달
  const result = await messageStore.sendMessage(props.receiverId, content.value, selectedFiles.value)
  
  if (result.success) {
    content.value = ''
    selectedFiles.value = []
    emit('success')
    emit('close')
  } else {
    errorMessage.value = result.message
  }
  isSending.value = false
}
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="message-modal-overlay" @click.self="$emit('close')">
      <div class="message-modal-content">
        <div class="modal-header">
          <h3>💌 쪽지 보내기</h3>
          <button class="close-btn" @click="$emit('close')">&times;</button>
        </div>
        
        <div class="receiver-info">
          <span class="label">수신자:</span>
          <span class="nickname">{{ receiverNickname }}</span>
        </div>

        <!-- [시니어 조치] 발송 정책 안내 바 -->
        <div class="message-policy-bar" :class="{ 'warning-border': isKarmaLow }">
          <div class="policy-item" :class="{ 'low-karma': isKarmaLow }">
            <span class="p-label">내 신뢰점수</span>
            <span class="p-value">{{ displayKarma }} P</span>
          </div>
          <div class="policy-divider"></div>
          <div class="policy-item">
            <span class="p-label">오늘 남은 발송</span>
            <span class="p-value highlight">{{ messageStore.dailyStatus.remainingCount }} / {{ messageStore.dailyStatus.maxCount }}</span>
          </div>
        </div>

        <div v-if="isKarmaLow" class="low-karma-warning">
          ⚠️ 신뢰 점수(Karma)가 80.0점 미만일 경우 쪽지 발송이 제한됩니다.
        </div>

        <div class="input-area">
          <textarea 
            v-model="content" 
            placeholder="상대방에게 전달할 내용을 입력하세요. 비방이나 욕설은 제재 대상이 될 수 있습니다."
            maxlength="500"
            :disabled="isSending || isKarmaLow"
          ></textarea>
          <div class="char-count">{{ content.length }} / 500</div>
        </div>

        <!-- 파일 첨부 영역 -->
        <div class="attachment-area">
          <label class="btn-attach" :class="{ 'disabled': isKarmaLow || isSending }">
            <input type="file" multiple @change="handleFileChange" accept="image/*,.pdf,.zip,.docx,.xlsx,.txt" :disabled="isKarmaLow || isSending" style="display: none;">
            <span class="attach-icon">📎</span> 파일 첨부 ({{ selectedFiles.length }}/5)
          </label>
        </div>
          
        <div v-if="selectedFiles.length > 0" class="file-list-preview">
          <div v-for="(file, idx) in selectedFiles" :key="idx" class="file-item-mini">
            <div class="file-preview-box" v-if="getFilePreview(file)">
              <img :src="getFilePreview(file)" alt="preview">
            </div>
            <div class="file-icon-placeholder" v-else>📄</div>
            <span class="file-name-text">{{ file.name }}</span>
            <button class="btn-remove-attach" @click="removeFile(idx)">&times;</button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-msg">⚠️ {{ errorMessage }}</div>

        <div class="modal-footer">
          <button class="cancel-btn" @click="$emit('close')" :disabled="isSending">취소</button>
          <button class="send-btn" @click="handleSend" :disabled="isSending || isKarmaLow || (!content.trim() && selectedFiles.length === 0)">
            <span v-if="isSending">보내는 중...</span>
            <span v-else>발송하기</span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.message-modal-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.7); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center; z-index: 10000;
}

.message-modal-content {
  width: 95%; max-width: 450px; background: var(--card-bg); border-radius: 24px;
  padding: 30px; box-shadow: 0 25px 50px rgba(0,0,0,0.5);
  border: 1.5px solid var(--border-color);
  animation: modal-pop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modal-pop {
  from { transform: scale(0.9) translateY(20px); opacity: 0; }
  to { transform: scale(1) translateY(0); opacity: 1; }
}

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.modal-header h3 { margin: 0; font-size: 1.25rem; font-weight: 900; color: var(--text-primary); }
.close-btn { background: none; border: none; font-size: 1.8rem; color: var(--text-muted); cursor: pointer; transition: 0.2s; }
.close-btn:hover { color: var(--text-primary); }

.receiver-info { 
  background: var(--hover-bg); padding: 12px 18px; border-radius: 12px; margin-bottom: 15px;
  display: flex; gap: 10px; align-items: center; border: 1px solid var(--border-color);
}
.receiver-info .label { font-size: 0.85rem; color: var(--text-secondary); font-weight: 700; }
.receiver-info .nickname { font-weight: 900; color: var(--link-color); font-size: 0.95rem; }

.message-policy-bar {
  display: flex; align-items: center; justify-content: space-around;
  background: var(--bg-primary); padding: 12px; border-radius: 12px;
  margin-bottom: 15px; border: 1px solid var(--border-color);
}
.policy-item { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.p-label { font-size: 0.65rem; color: var(--text-muted); font-weight: 700; }
.p-value { font-size: 0.85rem; font-weight: 850; color: var(--text-primary); }
.p-value.highlight { color: var(--link-color); }
.policy-divider { width: 1px; height: 20px; background: var(--border-color); }

.low-karma-warning {
  background: #fff0f0; color: #ff4d4d; font-size: 0.75rem; font-weight: 850;
  padding: 12px; border-radius: 12px; margin-bottom: 15px; text-align: center;
  border: 1px solid rgba(255, 77, 77, 0.2); animation: shake 0.5s;
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.warning-border { border-color: rgba(255, 77, 77, 0.5) !important; }
.policy-item.low-karma .p-value { color: #ff4d4d; }

.input-area { position: relative; }
textarea {
  width: 100%; height: 130px; background: var(--bg-primary); border: 1.5px solid var(--border-color);
  border-radius: 15px; padding: 15px; color: var(--text-primary); font-size: 0.95rem; resize: none;
  transition: all 0.3s; line-height: 1.6;
}
textarea:focus { border-color: var(--link-color); background: var(--card-bg); outline: none; }
textarea:disabled { opacity: 0.6; cursor: not-allowed; }

.char-count { position: absolute; bottom: 12px; right: 15px; font-size: 0.7rem; color: var(--text-muted); font-weight: 700; }

.attachment-area { margin-top: 15px; display: flex; justify-content: flex-end; }
.btn-attach { 
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 0.82rem; font-weight: 800; color: var(--link-color); cursor: pointer;
  padding: 8px 15px; background: var(--hover-bg); border-radius: 10px; transition: all 0.2s;
  border: 1px solid var(--border-color);
}
.btn-attach:hover:not(.disabled) { background: var(--border-color); transform: translateY(-1px); }
.btn-attach.disabled { opacity: 0.5; cursor: not-allowed; }

.file-list-preview { 
  margin-top: 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px; 
  max-height: 160px; overflow-y: auto; padding: 10px;
  background: var(--bg-primary); border-radius: 15px; border: 1px dashed var(--border-color);
}
.file-item-mini { 
  display: flex; align-items: center; gap: 10px; background: var(--card-bg);
  padding: 8px 12px; border-radius: 10px; border: 1px solid var(--border-color);
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}
.file-preview-box { width: 30px; height: 30px; border-radius: 6px; overflow: hidden; flex-shrink: 0; border: 1px solid var(--border-color); }
.file-preview-box img { width: 100%; height: 100%; object-fit: cover; }
.file-icon-placeholder { font-size: 1rem; flex-shrink: 0; }
.file-name-text { flex: 1; font-size: 0.75rem; font-weight: 700; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.btn-remove-attach { background: none; border: none; font-size: 1.2rem; color: var(--text-muted); cursor: pointer; padding: 0 2px; transition: 0.2s; }
.btn-remove-attach:hover { color: #ed4956; }

.error-msg { margin-top: 15px; color: #e74c3c; font-size: 0.8rem; font-weight: 800; text-align: center; }

.modal-footer { display: flex; gap: 12px; margin-top: 25px; }
.modal-footer button { flex: 1; padding: 14px; border-radius: 12px; font-weight: 900; cursor: pointer; transition: all 0.2s; border: none; font-size: 0.95rem; }

.cancel-btn { background: var(--hover-bg); color: var(--text-secondary); border: 1px solid var(--border-color); }
.send-btn { background: var(--link-color); color: white; box-shadow: 0 4px 15px rgba(52, 152, 219, 0.2); }
.send-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(52, 152, 219, 0.3); }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; filter: grayscale(0.5); }
</style>