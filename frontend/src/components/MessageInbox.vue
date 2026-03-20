<script setup>
import { onMounted, ref } from 'vue'
import { useMessageStore } from '@/stores/message'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'

const messageStore = useMessageStore()
const selectedMessage = ref(null)

onMounted(() => {
  if (messageStore.receivedMessages.length === 0) {
    messageStore.fetchReceivedMessages()
  }
})

const selectMessage = (msg) => {
  selectedMessage.value = msg
  if (!msg.isRead) {
    messageStore.markAsRead(msg.id)
  }
}

const handleReport = async (id) => {
  if (confirm('이 쪽지를 신고하시겠습니까? 운영자가 내용을 검토하게 됩니다.')) {
    const success = await messageStore.reportMessage(id)
    if (success) {
      alert('신고가 접수되었습니다.')
      selectedMessage.value.isReported = true
    }
  }
}

const handleBlock = async (userId, nickname) => {
  if (confirm(`[${nickname}]님을 차단하시겠습니까? 더 이상 이 유저의 쪽지를 받지 않게 됩니다.`)) {
    const success = await messageStore.blockUser(userId)
    if (success) {
      alert('차단되었습니다.')
    }
  }
}

const formatDate = (dateStr) => {
  return format(new Date(dateStr), 'yy.MM.dd HH:mm', { locale: ko })
}

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

const isImage = (type) => {
  return type && type.startsWith('image/');
}

const openImage = (url) => {
  window.open(url, '_blank');
}
</script>

<template>
  <div class="inbox-container">
    <!-- 왼쪽: 목록 -->
    <div class="message-list-side">
      <div class="list-header">
        <h2>📥 수신 쪽지함</h2>
        <span class="unread-badge" v-if="messageStore.unreadCount > 0">{{ messageStore.unreadCount }}</span>
      </div>
      
      <div v-if="messageStore.isLoading" class="loading-state">로딩 중...</div>
      <div v-else-if="messageStore.receivedMessages.length === 0" class="empty-state">
        <p>받은 쪽지가 없습니다.</p>
      </div>
      
      <div v-else class="list-items">
        <div 
          v-for="msg in messageStore.receivedMessages" 
          :key="msg.id" 
          class="message-item"
          :class="{ 'active': selectedMessage?.id === msg.id, 'unread': !msg.isRead, 'system-item': msg.isSystem }"
          @click="selectMessage(msg)"
        >
          <div class="item-info">
            <span class="sender">{{ msg.isSystem ? '📢 시스템' : msg.sender?.nickname || '(알 수 없음)' }}</span>
            <span class="date">{{ formatDate(msg.createdAt) }}</span>
          </div>
          <p class="preview">
            <span v-if="msg.files && msg.files.length > 0" class="clip-icon">📎</span>
            {{ msg.content }}
          </p>
        </div>
      </div>
    </div>

    <!-- 오른쪽: 내용 보기 -->
    <div class="message-detail-side">
      <div v-if="selectedMessage" class="detail-content" :class="{ 'system-detail': selectedMessage.isSystem }">
        <div class="detail-header">
          <div class="sender-info">
            <span class="sender-name">{{ selectedMessage.isSystem ? '📢 시스템 메시지' : selectedMessage.sender?.nickname }}</span>
            <span class="detail-date">{{ formatDate(selectedMessage.createdAt) }}</span>
          </div>
          <div class="actions" v-if="!selectedMessage.isSystem">
            <button class="action-btn report" @click="handleReport(selectedMessage.id)" :disabled="selectedMessage.isReported">
              {{ selectedMessage.isReported ? '신고됨' : '신고' }}
            </button>
            <button class="action-btn block" @click="handleBlock(selectedMessage.sender.id, selectedMessage.sender.nickname)">차단</button>
          </div>
        </div>
        
        <div class="content-body">
          {{ selectedMessage.content }}
        </div>

        <!-- 첨부파일 영역 -->
        <div v-if="selectedMessage.files && selectedMessage.files.length > 0" class="detail-attachments">
          <h4 class="attach-title">📎 첨부파일 ({{ selectedMessage.files.length }})</h4>
          <div class="attach-grid">
            <div v-for="file in selectedMessage.files" :key="file.id" class="attach-card">
              <template v-if="isImage(file.fileType)">
                <div class="attach-image-preview">
                  <img :src="file.fileUrl" alt="attachment" @click="openImage(file.fileUrl)">
                </div>
              </template>
              <div class="attach-info">
                <span class="file-name" :title="file.originalFileName">{{ file.originalFileName }}</span>
                <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
                <a :href="file.fileUrl" :download="file.originalFileName" class="btn-download" @click.stop>
                  📥 다운로드
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- AI 분석 주의 문구 -->
        <div v-if="selectedMessage.aiScore >= 0.7" class="ai-warning">
          ⚠️ AI가 분석한 결과, 불쾌감을 줄 수 있는 내용이 포함되어 있을 수 있습니다.
        </div>
      </div>
      
      <div v-else class="detail-empty">
        <div class="empty-icon">✉️</div>
        <p>확인할 쪽지를 선택해 주세요.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.inbox-container {
  display: flex; height: 600px; background: var(--card-bg); border-radius: 24px;
  border: 1.5 solid var(--border-color); overflow: hidden;
}

/* 목록 영역 */
.message-list-side {
  width: 350px; border-right: 1.5px solid var(--border-color);
  display: flex; flex-direction: column; background: var(--bg-primary);
}
.list-header { padding: 25px; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid var(--border-color); }
.list-header h2 { margin: 0; font-size: 1.2rem; font-weight: 900; color: var(--text-primary); }
.unread-badge { background: #e74c3c; color: white; font-size: 0.75rem; padding: 2px 8px; border-radius: 10px; font-weight: 800; }

.list-items { flex: 1; overflow-y: auto; }
.message-item { 
  padding: 20px; cursor: pointer; transition: all 0.2s; border-bottom: 1px solid var(--border-color);
  position: relative;
}
.message-item:hover { background: var(--hover-bg); }
.message-item.active { background: var(--card-bg); border-left: 4px solid var(--link-color); }
.message-item.unread::after { 
  content: ""; position: absolute; top: 22px; right: 20px; width: 8px; height: 8px; 
  background: #3498db; border-radius: 50%; 
}
.message-item.system-item { background: rgba(241, 196, 15, 0.03); }
.message-item.system-item.active { border-left-color: #f1c40f; }

.item-info { display: flex; justify-content: space-between; margin-bottom: 8px; }
.sender { font-weight: 850; font-size: 0.95rem; color: var(--text-primary); }
.date { font-size: 0.75rem; color: var(--text-muted); font-weight: 600; }
.preview { 
  margin: 0; font-size: 0.85rem; color: var(--text-secondary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  display: flex; align-items: center; gap: 4px;
}
.clip-icon { font-size: 0.9rem; }

/* 내용 영역 */
.message-detail-side { flex: 1; background: var(--card-bg); position: relative; }
.detail-content { padding: 40px; display: flex; flex-direction: column; height: 100%; overflow-y: auto; }
.detail-header { 
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 30px; padding-bottom: 20px; border-bottom: 1.5px solid var(--border-color);
  flex-shrink: 0;
}
.sender-name { font-size: 1.3rem; font-weight: 950; color: var(--text-primary); display: block; }
.detail-date { font-size: 0.85rem; color: var(--text-muted); font-weight: 600; margin-top: 5px; }

.actions { display: flex; gap: 10px; }
.action-btn { 
  padding: 6px 14px; border-radius: 8px; font-size: 0.8rem; font-weight: 800; cursor: pointer;
  border: 1.5px solid var(--border-color); background: var(--card-bg); transition: all 0.2s;
  color: var(--text-primary); /* 다크모드 대응: 검정 고정 대신 변수 사용 */
}
.action-btn.report:hover { color: #e74c3c; border-color: #e74c3c; }
.action-btn.block:hover { background: var(--hover-bg); border-color: var(--text-primary); }

.content-body { 
  font-size: 1.05rem; line-height: 1.7; color: var(--text-primary);
  white-space: pre-wrap; margin-bottom: 30px;
}

/* 첨부파일 스타일 */
.detail-attachments {
  margin-top: 25px; padding-top: 20px; border-top: 1px dashed var(--border-color);
}
.attach-title { font-size: 0.9rem; font-weight: 850; color: var(--text-secondary); margin: 0 0 15px; }
.attach-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.attach-card { 
  background: var(--bg-primary); border: 1px solid var(--border-color); border-radius: 12px;
  overflow: hidden; display: flex; flex-direction: column;
}
.attach-image-preview { width: 100%; height: 100px; cursor: zoom-in; overflow: hidden; }
.attach-image-preview img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
.attach-image-preview:hover img { transform: scale(1.1); }

.attach-info { padding: 10px; display: flex; flex-direction: column; gap: 4px; }
.attach-info .file-name { font-size: 0.75rem; font-weight: 700; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.attach-info .file-size { font-size: 0.65rem; color: var(--text-muted); font-weight: 600; }
.btn-download { 
  margin-top: 5px; font-size: 0.75rem; font-weight: 850; color: var(--link-color);
  text-decoration: none; display: flex; align-items: center; gap: 4px;
}
.btn-download:hover { text-decoration: underline; }

.system-detail { background: linear-gradient(to bottom, rgba(241, 196, 15, 0.05), transparent); }
.system-detail .sender-name { color: #f1c40f; }

.ai-warning { 
  margin-top: 20px; padding: 12px; background: rgba(231, 76, 60, 0.05); 
  border-radius: 10px; color: #e74c3c; font-size: 0.85rem; font-weight: 700; border: 1px solid rgba(231, 76, 60, 0.2);
}

.detail-empty { 
  height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: var(--text-muted); opacity: 0.5;
}
.empty-icon { font-size: 4rem; margin-bottom: 15px; }

@media (max-width: 992px) {
  .inbox-container { flex-direction: column; height: auto; min-height: 600px; }
  /* 모바일에서 목록 영역을 기존 300px에서 500px로 확장 */
  .message-list-side { width: 100%; height: 500px; border-right: none; border-bottom: 1.5px solid var(--border-color); }
  
  /* 모바일 첨부파일 1행 2개씩 노출 */
  .attach-grid { grid-template-columns: 1fr 1fr; }
  .attach-image-preview { height: 80px; }
  .detail-content { padding: 25px 20px; }
}
</style>
