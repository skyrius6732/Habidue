<script setup>
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import axios from '@/plugins/axios'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const isDarkMode = ref(localStorage.getItem('theme') === 'dark')
const isMenuOpen = ref(false)
const showTopBtn = ref(false) // TOP 버튼 상태
const searchKeyword = ref('') // [시니어 조치] 통합 검색어 상태

const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
  const theme = isDarkMode.value ? 'dark' : 'light'
  localStorage.setItem('theme', theme)
  applyTheme()
}

const applyTheme = () => {
  if (isDarkMode.value) {
    document.documentElement.classList.add('dark-mode')
  } else {
    document.documentElement.classList.remove('dark-mode')
  }
}

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

watch(() => route.path, () => {
  isMenuOpen.value = false
})

const logout = async () => {
  try {
    // 백엔드에 로그아웃 알림 (Redis 접속상태 제거용)
    await axios.post('/api/auth/logout')
  } catch (e) {
    console.error('로그아웃 API 호출 실패:', e)
  } finally {
    // 로컬 상태 및 토큰 정리
    authStore.clearTokens()
    isMenuOpen.value = false
    router.push('/')
  }
}

const goToLogin = () => {
  router.push('/')
}

// 1. 전체 메뉴(공고리스트 등) 노출 여부: 로그인 됨 + 특수 페이지(홈, 탈퇴성공, 차단페이지)가 아님
const isFullMenuVisible = computed(() => {
  return authStore.isAuthenticated && !['home', 'withdrawalSuccess', 'blocked'].includes(route.name)
})

// 2. 우측 상단 로그인 버튼 노출 여부: 로그인 안됨 + 이미 로그인 페이지(홈)가 아님 + 탈퇴성공/차단 페이지 아님
const isLoginButtonVisible = computed(() => {
  return !authStore.isAuthenticated && !['home', 'withdrawalSuccess', 'blocked'].includes(route.name)
})

// 전역 스크롤 관리
const scrollToTop = () => { window.scrollTo({ top: 0, behavior: 'smooth' }) }
const handleScroll = () => { showTopBtn.value = window.pageYOffset > 400 }

onMounted(() => {
  applyTheme()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <header class="app-header">
    <div class="header-content">
      <!-- 햄버거 버튼: 전체 메뉴가 보일 때만 노출 -->
      <button v-if="isFullMenuVisible" class="hamburger-btn" @click="toggleMenu">
        <span class="bar"></span>
        <span class="bar"></span>
        <span class="bar"></span>
      </button>

      <div class="header-left-group">
        <RouterLink to="/" class="logo-link">
          <h1 class="app-title">HabiDue</h1>
        </RouterLink>
        <!-- 테마 전환: 디자인 일관성을 위해 항상 노출 -->
        <button @click="toggleTheme" class="theme-toggle-btn desktop-only">
          {{ isDarkMode ? '☀️' : '🌙' }}
        </button>
      </div>

      <!-- 메인 네비게이션 -->
      <nav class="main-nav desktop-only">
        <!-- A. 로그인된 유저용 메뉴 -->
        <div v-if="isFullMenuVisible" class="nav-links-group">
          <div class="nav-links">
            <RouterLink to="/notices" :class="{ 'router-link-active': $route.path.startsWith('/notices') }">공고 리스트</RouterLink>
            <RouterLink to="/board" :class="{ 'router-link-active': $route.path.startsWith('/board') }">커뮤니티</RouterLink>
            <RouterLink to="/calendar" :class="{ 'router-link-active': $route.path.startsWith('/calendar') }">캘린더</RouterLink>
            <RouterLink to="/interests" :class="{ 'router-link-active': $route.path.startsWith('/interests') }">관심 공고</RouterLink>
            <RouterLink to="/keywords" :class="{ 'router-link-active': $route.path.startsWith('/keywords') }">마이 페이지</RouterLink>
            <RouterLink to="/about" :class="{ 'router-link-active': $route.path.startsWith('/about') }">HabiDue 소개</RouterLink>
            <RouterLink v-if="authStore.isAdmin" to="/admin" class="admin-link-highlight">관리자 페이지</RouterLink>
          </div>
          <button @click="logout" class="pc-action-btn logout">로그아웃</button>
        </div>

        <!-- B. 비로그인 유저용 로그인 유도 버튼 (홈/탈퇴 페이지 아닐 때만) -->
        <button v-else-if="isLoginButtonVisible" @click="goToLogin" class="pc-action-btn login">로그인</button>
      </nav>

      <!-- 모바일 전용 테마 버튼 (메뉴가 숨겨졌을 때 로고 우측 배치) -->
      <div v-if="!isFullMenuVisible" class="header-right-group mobile-only-flex">
        <button @click="toggleTheme" class="theme-toggle-btn">
          {{ isDarkMode ? '☀️' : '🌙' }}
        </button>
      </div>

      <div v-if="isFullMenuVisible" class="header-right-placeholder pc-hidden"></div>
    </div>

    <!-- 모바일 사이드바 -->
    <Transition name="slide">
      <div v-if="isMenuOpen && isFullMenuVisible" class="mobile-menu-overlay" @click="toggleMenu">
        <div class="mobile-menu-content" @click.stop>
          <div class="menu-header">
            <span class="menu-title">HabiDue</span>
            <button class="close-btn" @click="toggleMenu">&times;</button>
          </div>

          <!-- [시니어 조치] 모바일 메뉴 검색창 추가 -->
          <div v-if="isFullMenuVisible" class="mobile-search-area">
            <div class="search-input-wrapper mobile">
              <span class="search-icon">🔍</span>
              <input 
                type="text" 
                v-model="searchKeyword" 
                placeholder="어떤 정보가 필요하신가요?" 
                @keyup.enter="handleSearch"
                class="header-search-input"
              />
            </div>
          </div>

          <nav class="mobile-nav-list">
            <RouterLink to="/notices" :class="{ 'router-link-active': $route.path.startsWith('/notices') }"><span class="m-icon">🏠</span> 공고 리스트</RouterLink>
            <RouterLink to="/board" :class="{ 'router-link-active': $route.path.startsWith('/board') }"><span class="m-icon">💬</span> 커뮤니티</RouterLink>
            <RouterLink to="/calendar" :class="{ 'router-link-active': $route.path.startsWith('/calendar') }"><span class="m-icon">📅</span> 캘린더</RouterLink>
            <RouterLink to="/interests" :class="{ 'router-link-active': $route.path.startsWith('/interests') }"><span class="m-icon">❤️</span> 관심 공고</RouterLink>
            <RouterLink to="/keywords" :class="{ 'router-link-active': $route.path.startsWith('/keywords') }"><span class="m-icon">🔔</span> 마이 페이지</RouterLink>
            <RouterLink to="/about" :class="{ 'router-link-active': $route.path.startsWith('/about') }"><span class="m-icon">ℹ️</span> HabiDue 소개</RouterLink>
            
            <div class="mobile-nav-item" @click="toggleTheme">
              <span class="m-icon">{{ isDarkMode ? '☀️' : '🌙' }}</span>
              <span>테마 모드</span>
            </div>

            <div v-if="authStore.isAdmin" class="mobile-nav-item admin-mobile-link" @click="router.push('/admin')">
              <span class="m-icon">⚙️</span>
              <span>관리자 페이지</span>
            </div>

            <button @click="logout" class="mobile-logout-btn"><span class="m-icon">🚪</span> 로그아웃</button>
          </nav>
        </div>
      </div>
    </Transition>
  </header>

  <main class="app-main">
    <div class="content-wrapper">
      <RouterView />
    </div>
  </main>

  <!-- 퀵 TOP 버튼 (전역 유틸리티) -->
  <Transition name="fade-top">
    <button v-if="showTopBtn" class="floating-top-btn" @click="scrollToTop">
      <div class="top-arrow-icon">
        <span class="arrow-line"></span>
        <span class="arrow-line"></span>
      </div>
    </button>
  </Transition>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Grand+Hotel&display=swap');

:root {
  --bg-color: #fafafa;
  --header-bg: #ffffff;
  --card-bg: #ffffff;
  --text-primary: #262626;
  --text-secondary: #8e8e8e;
  --border-color: #dbdbdb;
  --divider-color: #dbdbdb;
  --link-color: #0095f6;
  --hover-bg: #f0f0f0;
  --tag-bg: #efefef;
  --profile-icon-bg: #efefef;
  --modal-overlay: rgba(0, 0, 0, 0.7);
}

.dark-mode {
  --bg-color: #121212;
  --header-bg: #1e1e1e;
  --card-bg: #1e1e1e;
  --text-primary: #f5f5f5;
  --text-secondary: #a8a8a8;
  --border-color: #444444;
  --divider-color: #555555;
  --link-color: #38bdf8;
  --hover-bg: #2c2c2c;
  --tag-bg: #333333;
  --profile-icon-bg: #ffffff;
  --modal-overlay: rgba(0, 0, 0, 0.85);
}

html { scrollbar-gutter: stable; }
body {
  -ms-overflow-style: none; scrollbar-width: none;
  margin: 0; padding: 0; display: block !important;
  background-color: var(--bg-color); color: var(--text-primary); transition: background-color 0.3s;
  /* [시니어] 전역 텍스트 뿌연 현상 해결을 위한 보정 */
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-shadow: none !important; /* 전체 텍스트 그림자 강제 제거 */
}
body::-webkit-scrollbar { display: none; }
#app { display: block !important; width: 100%; max-width: 100% !important; margin: 0 !important; padding: 0 !important; }

/* 전역 플로팅 TOP 버튼 스타일 */
.floating-top-btn { 
  display: none; 
  position: fixed; 
  bottom: 30px; 
  right: 20px; 
  width: 48px; 
  height: 48px; 
  background: var(--card-bg); 
  border: 1px solid var(--border-color); 
  border-radius: 50%; 
  box-shadow: 0 8px 24px rgba(0,0,0,0.12); 
  align-items: center; 
  justify-content: center; 
  cursor: pointer; 
  z-index: 3000; 
  transition: all 0.2s;
}
.floating-top-btn:active { transform: scale(0.9); }

.top-arrow-icon { display: flex; flex-direction: column; align-items: center; }
.arrow-line { 
  width: 10px; 
  height: 10px; 
  border-top: 2.5px solid var(--link-color); 
  border-left: 2.5px solid var(--link-color); 
  transform: rotate(45deg); 
  margin-bottom: -4.5px;
}

.fade-top-enter-active, .fade-top-leave-active { transition: opacity 0.3s; }
.fade-top-enter-from, .fade-top-leave-to { opacity: 0; }
</style>

<style scoped>
.app-header { background-color: var(--header-bg); position: sticky; top: 0; z-index: 100; height: 60px; width: 100%; display: flex; align-items: center; }
.header-content { max-width: 1200px; width: 100%; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; box-sizing: border-box; height: 100%; border-bottom: 1px solid var(--border-color); }

.header-left-group { display: flex; align-items: center; gap: 15px; flex-shrink: 0; }
.theme-toggle-btn { background: none; border: none; font-size: 1.1rem; cursor: pointer; color: var(--text-primary); }

/* [시니어 조치] 헤더 아이콘 버튼 공통 스타일 */
.header-icon-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
  color: var(--text-primary);
}
.header-icon-btn:hover { background-color: var(--hover-bg); }

.logo-link { text-decoration: none; color: inherit; }
.app-title { font-size: 1.8rem; font-family: 'Grand Hotel', cursive; margin: 0; color: var(--text-primary); }

.main-nav { display: flex; gap: 2rem; align-items: center; flex-grow: 1; justify-content: flex-end; }
.nav-links-group { display: flex; gap: 2rem; align-items: center; }
.nav-links { display: flex; gap: 1.5rem; align-items: center; }
.nav-links a { text-decoration: none; color: var(--text-primary); font-size: 0.95rem; font-weight: 500; white-space: nowrap; }
.nav-links a.router-link-active { color: var(--link-color); font-weight: 700; }

.admin-link-highlight { color: #ed4956 !important; font-weight: 800 !important; }
.admin-link-highlight:hover { text-decoration: underline; }

.admin-mobile-link { color: #ed4956 !important; font-weight: 700; }

.pc-action-btn { background-color: transparent; border: 1px solid var(--border-color); color: var(--text-secondary); padding: 6px 16px; border-radius: 6px; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.pc-action-btn:hover { background-color: var(--hover-bg); color: var(--link-color); border-color: var(--link-color); }
.pc-action-btn.logout:hover { color: #ed4956; border-color: #ed4956; }

.app-main { width: 100%; padding: 0; display: block; }
.content-wrapper { max-width: 1200px; margin: 0 auto; padding: 0; box-sizing: border-box; }

.hamburger-btn { display: none; flex-direction: column; gap: 4px; background: none; border: none; cursor: pointer; padding: 10px; flex-shrink: 0; }
.bar { width: 20px; height: 2px; background-color: var(--text-primary); border-radius: 2px; }

.mobile-menu-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0, 0, 0, 0.5); z-index: 3000; }
.mobile-menu-content { position: absolute; top: 0; left: 0; width: 260px; height: 100%; background-color: var(--header-bg); padding: 0; box-sizing: border-box; }
.menu-header { display: flex; justify-content: space-between; align-items: center; padding: 20px; border-bottom: 1px solid var(--border-color); }
.menu-title { font-family: 'Grand Hotel', cursive; font-size: 1.8rem; color: var(--text-primary); }
.close-btn { background: none; border: none; font-size: 1.8rem; color: var(--text-secondary); cursor: pointer; }

.mobile-nav-list { display: flex; flex-direction: column; }
.mobile-nav-list a, .mobile-logout-btn, .mobile-nav-item { 
  text-decoration: none; color: var(--text-primary); 
  font-size: 0.9rem; font-weight: 500; padding: 16px 20px; 
  border-bottom: 1px solid var(--border-color); 
  display: flex; align-items: center; gap: 12px; 
  cursor: pointer;
}
.mobile-nav-list a.router-link-active { background-color: var(--hover-bg); color: var(--link-color); font-weight: 700; }
.m-icon { font-size: 1.1rem; width: 24px; text-align: center; }
.mobile-logout-btn { background: none; border: none; color: #ed4956; width: 100%; text-align: left; }

/* [시니어 조치] 모바일 검색 스타일 */
.mobile-search-area { padding: 15px 20px; border-bottom: 1px solid var(--border-color); }
.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  background-color: var(--tag-bg);
  border-radius: 8px;
  padding: 0 12px;
  transition: all 0.2s;
}
.search-input-wrapper.mobile { background-color: var(--hover-bg); border: 1px solid var(--border-color); }
.search-icon { font-size: 0.9rem; color: var(--text-secondary); margin-right: 8px; }
.header-search-input {
  width: 100%;
  height: 36px;
  background: none;
  border: none;
  color: var(--text-primary);
  font-size: 0.9rem;
  outline: none;
}

.mobile-only-flex { display: none; }

.slide-enter-active, .slide-leave-active { transition: transform 0.3s ease; }
.slide-enter-from, .slide-leave-to { transform: translateX(-100%); }

@media (max-width: 768px) {
  .floating-top-btn { display: flex; }
  .desktop-only { display: none !important; }
  .hamburger-btn { display: flex; }
  .header-left-group { position: absolute; left: 50%; transform: translateX(-50%); }
  .pc-hidden { display: block; width: 40px; }
  .mobile-only-flex { display: flex; align-items: center; margin-left: auto; }
}
</style>
