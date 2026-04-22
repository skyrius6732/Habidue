<template>
  <div class="blocked-container">
    <div class="blocked-card">
      <div class="icon-area">🚫</div>
      <h1>계정이 차단되었습니다</h1>
      <p class="description">
        사용 정책 위반 또는 관리자의 판단에 의해<br />
        해당계정의 이용이 일시적 또는 영구적으로 제한되었습니다.
      </p>
      
      <div v-if="reason" class="reason-box">
        <span class="label">차단 사유</span>
        <p class="reason-text">{{ reason }}</p>
      </div>

      <div class="action-area">
        <p class="help-text">문의 사항이 있으시면 고객센터로 연락해 주시기 바랍니다.</p>
        <button @click="goHome" class="btn-home">홈으로 돌아가기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const reason = ref('')

onMounted(() => {
  // query.reason이 있으면 최우선 사용, 그 외에는 기본 메시지 사용
  const queryReason = route.query.reason || route.query.error
  if (queryReason && queryReason.trim()) {
    reason.value = queryReason
  } else {
    reason.value = '관리자의 판단에 의해 계정이 차단되었습니다.'
  }
  
  // 모든 인증 정보 삭제 (확실하게 즉시 튕겨내기)
  authStore.clearTokens()
})

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.blocked-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-color);
  padding: 20px;
}

.blocked-card {
  background: var(--card-bg);
  width: 100%;
  max-width: 450px;
  border-radius: 24px;
  padding: 40px;
  text-align: center;
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
  border: 1px solid var(--border-color);
}

.icon-area {
  font-size: 4rem;
  margin-bottom: 20px;
}

h1 {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 15px;
}

.description {
  color: var(--text-secondary);
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 30px;
}

.reason-box {
  background: var(--hover-bg);
  padding: 20px;
  border-radius: 16px;
  margin-bottom: 30px;
  text-align: left;
}

.reason-box .label {
  display: block;
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--link-color);
  margin-bottom: 8px;
  text-transform: uppercase;
}

.reason-text {
  color: var(--text-primary);
  font-size: 0.9rem;
  font-weight: 600;
  margin: 0;
  word-break: break-all;
}

.help-text {
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.btn-home {
  width: 100%;
  padding: 14px;
  background: var(--link-color);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 800;
  font-size: 1rem;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-home:hover {
  opacity: 0.9;
}
</style>
