<template>
  <div class="home-container">
    <div class="main-content">
      <div class="login-section">
        <div class="login-box">
          <h1 class="logo-font">HabiDue</h1>
          <p class="tagline">관심 있는 주거 공고를<br>가장 빠르게 확인하세요.</p>
          
          <div class="login-buttons">
            <button @click="loginWithGoogle" class="social-login-btn google">
              <div class="btn-inner">
                <div class="icon-wrapper">
                  <img src="@/assets/google-logo.png" alt="Google" class="btn-icon" />
                </div>
                <span class="btn-text">구글로 로그인</span>
              </div>
            </button>

            <button @click="loginWithKakao" class="social-login-btn kakao">
              <div class="btn-inner">
                <div class="icon-wrapper kakao-bg">
                  <span class="kakao-symbol">💬</span>
                </div>
                <span class="btn-text">카카오로 로그인</span>
              </div>
            </button>

            <button @click="loginWithNaver" class="social-login-btn naver">
              <div class="btn-inner">
                <div class="icon-wrapper naver-bg">
                  <span class="naver-symbol">N</span>
                </div>
                <span class="btn-text">네이버로 로그인</span>
              </div>
            </button>
          </div>

          <div class="divider">
            <div class="line"></div>
          </div>

          <p class="info-text">
            로그인하면 내 키워드에 맞는 공고를<br>실시간으로 확인할 수 있습니다.
          </p>
        </div>
        
        <div class="footer-box">
          <!-- 다크모드에서도 잘 보이도록 변수 적용 -->
          <p class="help-text">도움이 필요하신가요? <RouterLink to="/about" class="about-link">소개 페이지 보기</RouterLink></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

// [시니어 조치] 개발/운영 환경에 따른 베이스 URL 처리 (현재는 8081 하드코딩 유지)
const BASE_URL = 'http://localhost:8081';

const loginWithGoogle = () => {
  window.location.href = `${BASE_URL}/oauth2/authorization/google`;
};

const loginWithKakao = () => {
  window.location.href = `${BASE_URL}/oauth2/authorization/kakao`;
};

const loginWithNaver = () => {
  window.location.href = `${BASE_URL}/oauth2/authorization/naver`;
};

onMounted(() => {
  if (authStore.isAuthenticated) {
    router.push('/notices')
  }
})
</script>

<style scoped>
.home-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 100px);
}

.main-content {
  display: flex;
  justify-content: center;
  width: 100%;
  max-width: 935px;
}

.login-section {
  width: 350px;
}

.login-box {
  background-color: var(--card-bg);
  border: 1px solid var(--border-color);
  padding: 40px 30px;
  text-align: center;
  margin-bottom: 10px;
}

.logo-font {
  font-family: 'Grand Hotel', cursive;
  font-size: 3.5rem;
  margin-bottom: 15px;
  color: var(--text-primary);
}

.tagline {
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 1.1rem;
  line-height: 1.4;
  margin-bottom: 30px;
}

.login-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.social-login-btn {
  width: 100%;
  border: none;
  border-radius: 6px; /* 인스타 스타일의 둥근 모서리 보정 */
  padding: 10px 0;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 0.9rem;
  transition: transform 0.1s;
}

.social-login-btn:active {
  transform: scale(0.98);
}

.btn-inner {
  display: flex;
  align-items: center;
  width: 160px; /* 텍스트 정렬을 위해 너비 확장 */
  gap: 12px;
}

.google { background-color: var(--link-color); color: white; }
.kakao { background-color: #FEE500; color: #3C1E1E; }
.naver { background-color: #03C75A; color: white; }

.icon-wrapper {
  width: 24px;
  height: 24px;
  background: white;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kakao-bg { background-color: transparent; }
.naver-bg { background-color: transparent; }

.btn-icon { width: 16px; height: 16px; }
.kakao-symbol { font-size: 1.1rem; }
.naver-symbol { font-size: 0.9rem; font-weight: 900; }

.btn-text {
  text-align: left;
  flex-grow: 1;
}

.divider { display: flex; align-items: center; margin: 25px 0; }
.line { flex-grow: 1; height: 1px; background-color: var(--divider-color); }

.info-text { font-size: 0.85rem; color: var(--text-secondary); line-height: 1.5; }

/* 하단 도움말 박스 다크모드 대응 */
.footer-box { 
  background-color: var(--card-bg); 
  border: 1px solid var(--border-color); 
  padding: 20px; 
  text-align: center; 
  font-size: 0.9rem; 
}

/* 요청하신 텍스트 색상 조정: 다크모드에서도 명확하게 보이도록 var 사용 */
.help-text {
  color: var(--text-primary);
  margin: 0;
}

.about-link { 
  color: var(--text-primary); /* 소개페이지 보기 텍스트를 검정색(다크모드는 흰색)으로 요청 */
  text-decoration: none; 
  font-weight: 600; 
}

.about-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .main-content { padding: 0 20px; }
  .login-section { width: 100%; max-width: 350px; }
}
</style>
