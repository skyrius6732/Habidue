<template>
  <div class="error-container">
    <div class="error-content">
      <div class="error-icon">🏠?</div>
      <h1 class="error-title">길을 잃으셨나요?</h1>
      <p class="error-text">
        요청하신 페이지가 존재하지 않거나,<br>
        삭제되었을 수 있습니다.
      </p>
      <div class="error-actions">
        <button @click="goHome" class="btn-home">홈으로 돌아가기</button>
        <button @click="goBack" class="btn-back">이전 페이지</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  padding: 20px;
}

.error-content {
  text-align: center;
  max-width: 400px;
  width: 100%;
}

.error-icon {
  font-size: 5rem;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.error-title {
  font-size: 1.8rem;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 15px;
}

.error-text {
  font-size: 1rem;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 40px;
}

.error-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.btn-home {
  background-color: var(--link-color);
  color: white;
  border: none;
  padding: 14px;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-home:hover {
  opacity: 0.9;
}

.btn-back {
  background: none;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  padding: 12px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
}

.btn-back:hover {
  background-color: var(--hover-bg);
}
</style>
