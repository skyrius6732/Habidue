<script setup>
import MessageInbox from '@/components/MessageInbox.vue'
</script>

<template>
  <div class="message-view-container">
    <div class="view-header">
      <h1 class="view-title">📬 쪽지함</h1>
      <p class="view-desc">사용자 및 시스템으로부터 도착한 메시지를 확인하세요.</p>
    </div>
    
    <div class="view-content">
      <MessageInbox />
    </div>
  </div>
</template>

<style scoped>
.message-view-container {
  padding: 30px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.view-header {
  margin-bottom: 30px;
}

.view-title {
  font-size: 1.8rem;
  font-weight: 950;
  color: var(--text-primary);
  margin: 0;
}

.view-desc {
  font-size: 1rem;
  color: var(--text-secondary);
  margin: 8px 0 0;
  font-weight: 500;
}

.view-content {
  margin-top: 20px;
}

@media (max-width: 768px) {
  .message-view-container {
    padding: 20px 10px;
  }
  .view-title {
    font-size: 1.5rem;
  }
}
</style>
