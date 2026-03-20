<script setup>
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { computed } from 'vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const logout = () => {
  authStore.clearTokens()
  router.push('/')
}

const isLandingPage = computed(() => route.path === '/' && !authStore.isAuthenticated)
</script>

<template>
  <header class="app-header">
    <div class="header-content">
      <RouterLink to="/" class="logo-link">
        <h1 class="app-title">HabiDue</h1>
      </RouterLink>

      <nav v-if="!isLandingPage" class="main-nav">
        <RouterLink v-if="authStore.isAuthenticated" to="/notices">공고 목록</RouterLink>
        <RouterLink v-if="authStore.isAuthenticated" to="/calendar">캘린더</RouterLink>
        <RouterLink v-if="authStore.isAuthenticated" to="/interests">관심 공고</RouterLink>
        <RouterLink v-if="authStore.isAuthenticated" to="/keywords">설정</RouterLink>
        <RouterLink to="/about">소개</RouterLink>
        <button v-if="authStore.isAuthenticated" @click="logout" class="logout-btn">로그아웃</button>
      </nav>
    </div>
  </header>

  <main class="app-main">
    <div class="content-wrapper">
      <RouterView />
    </div>
  </main>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Grand+Hotel&display=swap');

html { scrollbar-gutter: stable; }
body {
  margin: 0; padding: 0; display: block !important;
  background-color: #fafafa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #262626;
  min-height: 100vh;
}
#app { display: block !important; width: 100%; }
</style>

<style scoped>
.app-header {
  background-color: #ffffff;
  border-bottom: 1px solid #dbdbdb;
  position: sticky; top: 0; z-index: 100;
  height: 60px; width: 100%;
  display: flex; align-items: center;
}

.header-content {
  max-width: 1200px; /* 헤더 너비를 1200px로 확장 */
  width: 100%; margin: 0 auto;
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 20px; box-sizing: border-box;
}

.logo-link { text-decoration: none; color: inherit; flex-shrink: 0; }
.app-title { font-size: 1.8rem; font-weight: 400; color: #262626; margin: 0; font-family: 'Grand Hotel', cursive; }

.main-nav { display: flex; gap: 1.5rem; align-items: center; }
.main-nav a { text-decoration: none; color: #262626; font-size: 0.95rem; font-weight: 500; white-space: nowrap; }
.main-nav a.router-link-active { color: #0095f6; font-weight: 700; }
.logout-btn { background: none; border: none; color: #8e8e8e; font-weight: 600; font-size: 0.9rem; cursor: pointer; white-space: nowrap; }

.app-main { width: 100%; padding: 30px 0; display: block; }

.content-wrapper {
  max-width: 1200px; /* 전체 콘텐츠 가용 너비 1200px로 확장 */
  margin: 0 auto; padding: 0 20px; box-sizing: border-box;
}

/* 개별 페이지 컨테이너들도 1200px 기준으로 확장 */
:deep(.interests-container), 
:deep(.calendar-container), 
:deep(.settings-container),
:deep(.about-container),
:deep(.feed-container) {
  max-width: 1200px !important; 
  margin: 0 auto;
}

@media (max-width: 768px) {
  .app-header { height: auto; padding: 10px 0; }
  .header-content { flex-direction: column; gap: 8px; padding: 0 10px; }
  .main-nav { width: 100%; justify-content: center; gap: 0.7rem; flex-wrap: nowrap; }
  .main-nav a { font-size: 0.75rem; }
  .app-main { padding-top: 10px; }
  .content-wrapper { max-width: 100%; padding: 0 10px; }
}
</style>
