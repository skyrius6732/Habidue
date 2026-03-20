<script setup>
import { ref } from 'vue'
import { useMessageStore } from '@/stores/message'

const props = defineProps({
  show: Boolean,
  receiverId: [Number, String],
  receiverNickname: String
})

const emit = defineEmits(['close', 'success'])
const messageStore = useMessageStore()

const content = ref('')
const selectedFiles = ref([])
const isSending = ref(false)
const errorMessage = ref('')

const handleFileChange = (e) => {
  const files = Array.from(e.target.files)
  // 최대 5개 파일 제한
  if (selectedFiles.value.length + files.length > 5) {
    alert('파일은 최대 5개까지만 첨부할 수 있습니다.')
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
  
  isSending.value = true
  errorMessage.value = ''
  
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

        <div class="input-area">
          <textarea 
            v-model="content" 
            placeholder="상대방에게 전달할 내용을 입력하세요. 비방이나 욕설은 제재 대상이 될 수 있습니다."
            maxlength="500"
            :disabled="isSending"
          ></textarea>
          <div class="char-count">{{ content.length }} / 500</div>
        </div>

        <!-- 파일 첨부 영역 -->
        <div class="attachment-area">
          <label class="btn-attach">
            <input type="file" multiple @change="handleFileChange" accept="image/*,.pdf,.zip,.docx,.xlsx,.txt" style="display: none;">
            <span class="attach-icon">📎</span> 파일 첨부 ({{ selectedFiles.length }}/5)
          </label>
          
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
        </div>

        <div v-if="errorMessage" class="error-msg">⚠️ {{ errorMessage }}</div>

        <div class="modal-footer">
          <button class="cancel-btn" @click="$emit('close')" :disabled="isSending">취소</button>
          <button class="send-btn" @click="handleSend" :disabled="isSending || (!content.trim() && selectedFiles.length === 0)">
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
  width: 95%; max-width: 450px; background: var(--card-bg); border-radius: 20px;
  padding: 25px; box-shadow: 0 25px 50px rgba(0,0,0,0.5);
  border: 1.5px solid var(--border-color);
  animation: modal-pop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modal-pop {
  from { transform: scale(0.9) translateY(20px); opacity: 0; }
  to { transform: scale(1) translateY(0); opacity: 1; }
}

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.modal-header h3 { margin: 0; font-size: 1.25rem; font-weight: 900; color: var(--text-primary); }
.close-btn { background: none; border: none; font-size: 1.8rem; color: var(--text-muted); cursor: pointer; }

.receiver-info { 
  background: var(--hover-bg); padding: 12px 15px; border-radius: 12px; margin-bottom: 15px;
  display: flex; gap: 10px; align-items: center;
}
.receiver-info .label { font-size: 0.85rem; color: var(--text-secondary); font-weight: 700; }
.receiver-info .nickname { font-weight: 900; color: var(--link-color); font-size: 0.95rem; }

.input-area { position: relative; }
textarea {
  width: 100%; height: 120px; background: var(--bg-primary); border: 1.5px solid var(--border-color);
  border-radius: 12px; padding: 15px; color: var(--text-primary); font-size: 0.95rem; resize: none;
  transition: all 0.3s; line-height: 1.5;
}
textarea:focus { border-color: var(--link-color); outline: none; }

.char-count { position: absolute; bottom: 10px; right: 10px; font-size: 0.7rem; color: var(--text-muted); font-weight: 600; }

/* 파일 첨부 섹션 */
.attachment-area { margin-top: 15px; }
.btn-attach { 
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 0.85rem; font-weight: 800; color: var(--link-color); cursor: pointer;
  padding: 8px 12px; background: var(--hover-bg); border-radius: 8px; transition: all 0.2s;
}
.btn-attach:hover { background: var(--border-color); }
.attach-icon { font-size: 1.1rem; }

.file-list-preview { 
  margin-top: 12px; display: flex; flex-direction: column; gap: 6px; 
  max-height: 120px; overflow-y: auto; padding-right: 5px;
}
.file-item-mini { 
  display: flex; align-items: center; gap: 10px; background: var(--bg-primary);
  padding: 6px 10px; border-radius: 8px; border: 1px solid var(--border-color);
}
.file-preview-box { width: 22px; height: 22px; border-radius: 4px; overflow: hidden; flex-shrink: 0; }
.file-preview-box img { width: 100%; height: 100%; object-fit: cover; }
.file-icon-placeholder { font-size: 0.9rem; flex-shrink: 0; }
.file-name-text { flex: 1; font-size: 0.75rem; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.btn-remove-attach { background: none; border: none; font-size: 1.1rem; color: var(--text-muted); cursor: pointer; padding: 0 4px; }
.btn-remove-attach:hover { color: #ed4956; }

.error-msg { margin-top: 12px; color: #e74c3c; font-size: 0.8rem; font-weight: 700; text-align: center; }

.modal-footer { display: flex; gap: 12px; margin-top: 20px; }
.modal-footer button { flex: 1; padding: 12px; border-radius: 10px; font-weight: 850; cursor: pointer; transition: all 0.2s; border: none; }

.cancel-btn { background: var(--hover-bg); color: var(--text-secondary); }
.send-btn { background: var(--link-color); color: white; }
.send-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 5px 15px rgba(52, 152, 219, 0.3); }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
