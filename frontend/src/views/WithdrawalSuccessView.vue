<template>
  <div class="success-container">
    <div class="success-card fade-in">
      <div class="icon-circle">🕊️</div>
      <h1 class="title">그동안 HabiDue와<br>함께해 주셔서 감사합니다.</h1>
      <p class="desc">
        사용자님의 계정과 모든 활동 데이터가<br>
        안전하게 삭제되었습니다.
      </p>

      <div class="info-box">
        <h3 class="info-title">💡 안내 사항</h3>
        <p class="info-text">
          서버의 정보는 삭제되었으나 구글 계정 권한은 유지될 수 있습니다. 완전한 해제는 아래 링크를 이용해 주세요.
        </p>
        <a href="https://myaccount.google.com/permissions" target="_blank" class="btn-google-link">
          구글 연동 권한 해제하기 ↗
        </a>
      </div>

      <div class="action-group">
        <router-link to="/" class="btn-login-again">로그인 페이지 이동</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

onMounted(() => {
  authStore.clearTokens()
})
</script>

<style scoped>
.success-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--hover-bg);
  padding: 15px;
}

.success-card {
  background: var(--card-bg);
  width: 100%;
  max-width: 460px;
  padding: 40px 25px; /* 패딩 축소 */
  border-radius: 20px;
  border: 1px solid var(--border-color);
  text-align: center;
  box-shadow: 0 15px 35px rgba(0,0,0,0.05);
}

.icon-circle {
  font-size: 3rem;
  margin-bottom: 20px;
}

.title {
  font-size: 1.4rem; /* 폰트 축소 */
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.3;
  margin-bottom: 15px;
  word-break: keep-all;
}

.desc {
  font-size: 0.95rem;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 25px;
  word-break: keep-all;
}

.info-box {
  background-color: var(--hover-bg);
  border-radius: 12px;
  padding: 18px;
  margin-bottom: 30px;
  text-align: left;
  border: 1px dashed var(--border-color);
}

.info-title {
  font-size: 0.9rem;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.info-text {
  font-size: 0.8rem;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 10px;
}

.btn-google-link {
  display: inline-block;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--link-color);
  text-decoration: none;
  border-bottom: 1px solid var(--link-color);
}

.btn-login-again {
  display: block;
  width: 100%;
  padding: 14px;
  background-color: var(--link-color);
  color: white;
  text-decoration: none;
  border-radius: 10px;
  font-weight: 800;
  font-size: 1rem;
  transition: all 0.2s;
}

.fade-in {
  animation: fadeIn 0.8s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .success-card { padding: 30px 20px; }
  .title { font-size: 1.25rem; }
  .desc { font-size: 0.9rem; }
  .info-box { padding: 15px; margin-bottom: 25px; }
}
</style>
