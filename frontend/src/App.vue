<script setup>
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { formatDistanceToNow } from 'date-fns'
import { ko } from 'date-fns/locale'
import axios from '@/plugins/axios'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const router = useRouter()
const route = useRoute()

const isDarkMode = ref(localStorage.getItem('theme') === 'dark')
const isMenuOpen = ref(false)
const showTopBtn = ref(false)
const searchKeyword = ref('')

// [시니어 조치] 실시간 토스트 알림 상태
const showToast = ref(false)
const latestNoti = ref(null)
let toastTimer = null

const triggerToast = (noti) => {
  latestNoti.value = noti
  showToast.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { showToast.value = false }, 5000)
}

const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
  const theme = isDarkMode.value ? 'dark' : 'light'
  localStorage.setItem('theme', theme)
  applyTheme()
}

const applyTheme = () => {
  if (isDarkMode.value) document.documentElement.classList.add('dark-mode')
  else document.documentElement.classList.remove('dark-mode')
}

const toggleMenu = () => { isMenuOpen.value = !isMenuOpen.value }

watch(() => route.path, () => { isMenuOpen.value = false })

const logout = async () => {
  try { await axios.post('/api/auth/logout') } catch (e) {}
  finally {
    authStore.clearTokens()
    notificationStore.disconnectSse()
    isMenuOpen.value = false
    router.push('/')
  }
}

const goToLogin = () => { router.push('/') }
const isNotiOpen = ref(false)
const toggleNoti = () => {
  isNotiOpen.value = !isNotiOpen.value
  // [시니어 조치] 알림 목록이 비어있을 때만 최초 1회 페칭 (이후엔 SSE가 실시간 동기화)
  // 매번 페칭하면 읽음 처리 중인 서버의 이전 데이터를 가져와 로컬 상태를 덮어쓰는 문제가 발생함
  if (isNotiOpen.value && notificationStore.notifications.length === 0) {
    notificationStore.fetchNotifications()
  }
}
const handleNotiClick = async (noti) => {
  await notificationStore.markAsRead(noti.id)
  isNotiOpen.value = false
  showToast.value = false

  if (noti.type === 'COMMENT' || noti.type === 'REPLY') {
    // [시니어 조치] 상세 페이지로 이동하며 댓글 ID를 쿼리로 전달
    router.push({
      path: `/board/post/${noti.postId}`,
      query: { commentId: noti.relatedTargetId, t: Date.now() }
    })
  } else if (noti.type === 'MESSAGE') {
    router.push({ path: '/keywords', query: { tab: 'messages', t: Date.now() } })
  } else if (noti.type === 'NOTICE_KEYWORD' || noti.type === 'NOTICE_DEADLINE' || noti.type === 'NOTICE_STATUS_CHANGE') {
    // [시니어 조치] 공고 관련 알림 클릭 시 상세 모달 자동 오픈을 위해 openId 전달
    // 키워드 검색창 자동 입력을 위해 keyword도 함께 전달
    const targetQuery = { openId: noti.relatedTargetId, t: Date.now() };

    // 키워드 알림인 경우 제목에서 앞의 [기관] 부분을 제외한 핵심 키워드 추출 시도
    if (noti.type === 'NOTICE_KEYWORD') {
      const titleMatch = noti.content.match(/등록되었습니다: (.*)/);
      if (titleMatch && titleMatch[1]) {
        targetQuery.keyword = titleMatch[1].substring(0, 15); // 제목 앞부분 15자 정도만 검색어로 사용
      }
    }

    router.push({ path: '/notices', query: targetQuery })
  } else if (noti.type === 'KARMA_CHANGE' || noti.type === 'SYSTEM') {
    // [시니어 조치] SYSTEM 타입(좋아요 등)이 게시글 관련이면 해당 위치로 이동
    if (noti.postId) {
      router.push({
        path: `/board/post/${noti.postId}`,
        query: { 
          commentId: (noti.relatedTargetId && noti.relatedTargetId !== noti.postId) ? noti.relatedTargetId : null, 
          t: Date.now() 
        }
      })
    } else if (noti.content.includes('쪽지')) {
      // [시니어 조치] 쪽지 제재 알림인 경우 마이페이지 > 쪽지함으로 이동
      router.push({ path: '/keywords', query: { tab: 'messages', t: Date.now() } })
    } else if (noti.content.includes('[공지]')) {
      // [시니어 조치] 시스템 공지 알림인 경우 HabiDue 소개 > 공지사항 탭으로 이동
      router.push({ path: '/about', query: { tab: 'news', t: Date.now() } })
    } else if (noti.content && noti.content.includes('답변이 등록되었습니다')) {
      // 고객센터 문의 답변 알림 → 소개 > 고객센터 탭 > 해당 문의 포커싱
      router.push({ path: '/about', query: { tab: 'support', inquiryId: noti.relatedTargetId, t: Date.now() } })
    } else {
      router.push({ path: '/keywords', query: { tab: 'activity', t: Date.now() } })
    }
  }
}
const timeAgo = (date) => formatDistanceToNow(new Date(date), { addSuffix: true, locale: ko })

const handleNotiScroll = (e) => {
  const { scrollTop, scrollHeight, clientHeight } = e.target
  // 바닥에서 10px 정도 여유를 두고 감지
  if (scrollTop + clientHeight >= scrollHeight - 10) {
    if (notificationStore.hasMore && !notificationStore.isLoadingMore) {
      notificationStore.fetchNotifications(false)
    }
  }
}

const isFullMenuVisible = computed(() => authStore.isAuthenticated && !['home', 'withdrawalSuccess', 'blocked'].includes(route.name))
const isLoginButtonVisible = computed(() => !authStore.isAuthenticated && !['home', 'withdrawalSuccess', 'blocked'].includes(route.name))

const scrollToTop = () => { window.scrollTo({ top: 0, behavior: 'smooth' }) }
const handleScroll = () => { showTopBtn.value = window.pageYOffset > 400 }

const vClickOutside = {
  mounted(el, binding) {
    el.clickOutsideEvent = (e) => { if (!(el === e.target || el.contains(e.target))) binding.value(e) }
    document.addEventListener("click", el.clickOutsideEvent)
  },
  unmounted(el) { document.removeEventListener("click", el.clickOutsideEvent) },
}

const isMobile = ref(false)
const updateIsMobile = () => { isMobile.value = window.innerWidth <= 768 }

onMounted(() => {
  applyTheme()
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
  window.addEventListener('scroll', handleScroll)
  if (authStore.isAuthenticated) {
    notificationStore.initialize((noti) => triggerToast(noti))
  }
})

watch(() => authStore.isAuthenticated, (val) => {
  if (val) {
    notificationStore.initialize((noti) => triggerToast(noti))
  } else {
    notificationStore.disconnectSse()
    notificationStore.stopPolling()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', updateIsMobile)
  window.removeEventListener('scroll', handleScroll)
  notificationStore.disconnectSse()
  notificationStore.stopPolling()
})
</script>

<template>
  <header class="app-header">
    <div class="header-content">
      <button v-if="isFullMenuVisible" class="hamburger-btn" @click="toggleMenu"><span class="bar"></span><span class="bar"></span><span class="bar"></span></button>
      <div class="header-left-group">
        <RouterLink to="/" class="logo-link"><h1 class="app-title">HabiDue</h1></RouterLink>
        <button @click="toggleTheme" class="theme-toggle-btn desktop-only">{{ isDarkMode ? '☀️' : '🌙' }}</button>
      </div>
      <nav class="main-nav desktop-only">
        <div v-if="isFullMenuVisible" class="nav-links-group">
          <div class="nav-links">
            <RouterLink to="/notices">공고 리스트</RouterLink>
            <RouterLink to="/board">커뮤니티</RouterLink>
            <RouterLink to="/calendar">캘린더</RouterLink>
            <RouterLink to="/interests">관심 공고</RouterLink>
            <RouterLink to="/keywords">마이 페이지</RouterLink>
            <RouterLink to="/about">HabiDue 소개</RouterLink>
            <RouterLink v-if="authStore.isAdmin" to="/admin" class="admin-link-highlight">관리자 페이지</RouterLink>
          </div>
          <div v-if="!isMobile" class="header-noti-container" v-click-outside="() => isNotiOpen = false">
            <button class="header-icon-btn noti-btn" @click="toggleNoti">
              <span class="noti-icon">🔔</span>
              <span v-if="notificationStore.unreadCount > 0" class="unread-badge">{{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}</span>
            </button>
            <Transition name="fade-down">
              <div v-if="isNotiOpen" class="noti-dropdown">
                <div class="noti-header"><span class="noti-title">알림</span><button v-if="notificationStore.notifications.some(n => !n.isRead)" class="all-read-btn" @click="notificationStore.markAllAsRead">모두 읽음</button></div>
                <div class="noti-list thin-scrollbar" @scroll="handleNotiScroll">
                  <div v-if="notificationStore.notifications.length === 0" class="empty-noti">새로운 알림이 없습니다.</div>
                  <div v-for="noti in notificationStore.notifications" :key="noti.id" class="noti-item" :class="{ 'is-unread': !noti.isRead }" @click="handleNotiClick(noti)">
                    <span class="noti-type-icon">{{ noti.icon }}</span>
                    <div class="noti-body"><p class="noti-content">{{ noti.content }}</p><span class="noti-time">{{ timeAgo(noti.createdAt) }}</span></div>
                    <span v-if="!noti.isRead" class="unread-dot"></span>
                  </div>
                  <!-- 로딩 인디케이터 (무한 스크롤용) -->
                  <div v-if="notificationStore.isLoadingMore" class="noti-more-container">
                    <span class="loading-spinner-small"></span>
                  </div>
                </div>
              </div>
            </Transition>
          </div>
          <button @click="logout" class="pc-action-btn logout">로그아웃</button>
        </div>
        <button v-else-if="isLoginButtonVisible" @click="goToLogin" class="pc-action-btn login">로그인</button>
      </nav>

      <!-- [시니어 조치] 모바일 전용 우측 그룹 (알림만 노출) -->
      <div class="header-right-group mobile-only-flex">
        <div v-if="isFullMenuVisible && isMobile" class="header-noti-container" v-click-outside="() => isNotiOpen = false">
          <button class="header-icon-btn noti-btn" @click="toggleNoti">
            <span class="noti-icon">🔔</span>
            <span v-if="notificationStore.unreadCount > 0" class="unread-badge">{{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}</span>
          </button>
          <Transition name="fade-down">
            <div v-if="isNotiOpen" class="noti-dropdown">
              <div class="noti-header"><span class="noti-title">알림</span><button v-if="notificationStore.notifications.some(n => !n.isRead)" class="all-read-btn" @click="notificationStore.markAllAsRead">모두 읽음</button></div>
              <div class="noti-list thin-scrollbar" @scroll="handleNotiScroll">
                <div v-if="notificationStore.notifications.length === 0" class="empty-noti">새로운 알림이 없습니다.</div>
                <div v-for="noti in notificationStore.notifications" :key="noti.id" class="noti-item" :class="{ 'is-unread': !noti.isRead }" @click="handleNotiClick(noti)">
                  <span class="noti-type-icon">{{ noti.icon }}</span>
                  <div class="noti-body"><p class="noti-content">{{ noti.content }}</p><span class="noti-time">{{ timeAgo(noti.createdAt) }}</span></div>
                  <span v-if="!noti.isRead" class="unread-dot"></span>
                </div>
                <!-- 로딩 인디케이터 (무한 스크롤용) -->
                <div v-if="notificationStore.isLoadingMore" class="noti-more-container">
                  <span class="loading-spinner-small"></span>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </div>
  </header>

  <!-- [시니어 조치] 모바일 전용 사이드바 메뉴 및 오버레이 -->
  <Transition name="fade">
    <div v-if="isMenuOpen" class="sidebar-overlay" @click="toggleMenu"></div>
  </Transition>
  <Transition name="slide">
    <aside v-if="isMenuOpen" class="app-sidebar insta-style">
      <div class="sidebar-inner">
        <div class="sidebar-header">
          <span class="sidebar-id">habidue_official</span>
          <button class="sidebar-close-btn" @click="toggleMenu">
            <svg viewBox="0 0 24 24" width="24" height="24" stroke="currentColor" stroke-width="2" fill="none"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>
        
        <div class="sidebar-profile-section" v-if="authStore.isAuthenticated">
          <div class="profile-avatar">
            {{ authStore.user?.nickname?.charAt(0) || 'U' }}
          </div>
          <div class="profile-info">
            <span class="profile-nickname">{{ authStore.user?.nickname }}</span>
            <span class="profile-sub">Level {{ authStore.user?.level || 1 }} Active Member</span>
          </div>
        </div>

        <nav class="sidebar-nav-grid">
          <template v-if="isFullMenuVisible">
            <RouterLink to="/notices" @click="isMenuOpen = false" class="nav-item">
              <span class="nav-icon">📋</span>
              <span class="nav-text">공고 리스트</span>
            </RouterLink>
            <RouterLink to="/board" @click="isMenuOpen = false" class="nav-item">
              <span class="nav-icon">💬</span>
              <span class="nav-text">커뮤니티</span>
            </RouterLink>
            <RouterLink to="/calendar" @click="isMenuOpen = false" class="nav-item">
              <span class="nav-icon">📅</span>
              <span class="nav-text">캘린더</span>
            </RouterLink>
            <RouterLink to="/interests" @click="isMenuOpen = false" class="nav-item">
              <span class="nav-icon">⭐</span>
              <span class="nav-text">관심 공고</span>
            </RouterLink>
            <RouterLink to="/keywords" @click="isMenuOpen = false" class="nav-item">
              <span class="nav-icon">👤</span>
              <span class="nav-text">마이 페이지</span>
            </RouterLink>
            <RouterLink to="/about" @click="isMenuOpen = false" class="nav-item">
              <span class="nav-icon">ℹ️</span>
              <span class="nav-text">HabiDue 소개</span>
            </RouterLink>
          </template>
        </nav>

        <div class="sidebar-footer">
          <button @click="toggleTheme" class="footer-action-btn">
            <span class="footer-icon">{{ isDarkMode ? '☀️' : '🌙' }}</span>
            <span>{{ isDarkMode ? '라이트 모드' : '다크 모드' }}</span>
          </button>
          <div v-if="authStore.isAdmin" class="footer-divider"></div>
          <RouterLink v-if="authStore.isAdmin" to="/admin" class="footer-action-btn admin" @click="isMenuOpen = false">
            <span class="footer-icon">⚙️</span>
            <span>관리자 도구</span>
          </RouterLink>
          <div class="footer-divider"></div>
          <button v-if="authStore.isAuthenticated" @click="logout" class="footer-action-btn logout">
            <span class="footer-icon">👋</span>
            <span>로그아웃</span>
          </button>
          <button v-else-if="isLoginButtonVisible" @click="goToLogin" class="footer-action-btn login">
            <span class="footer-icon">🔑</span>
            <span>로그인</span>
          </button>
        </div>
      </div>
    </aside>
  </Transition>

  <main class="app-main"><div class="content-wrapper"><RouterView /></div></main>

  <Transition name="fade-top"><button v-if="showTopBtn" class="floating-top-btn" @click="scrollToTop"><div class="top-arrow-icon"><span class="arrow-line"></span><span class="arrow-line"></span></div></button></Transition>

  <!-- [시니어 조치] 실시간 알림 토스트 -->
  <Transition name="toast">
    <div v-if="showToast && latestNoti" class="noti-toast" @click="handleNotiClick(latestNoti)">
      <span class="t-icon">{{ latestNoti.icon }}</span>
      <div class="t-content"><span class="t-label">새로운 알림</span><p class="t-text">{{ latestNoti.content }}</p></div>
      <button class="t-close" @click.stop="showToast = false">&times;</button>
    </div>
  </Transition>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Grand+Hotel&display=swap');
:root { --bg-color: #fafafa; --header-bg: #ffffff; --card-bg: #ffffff; --text-primary: #262626; --text-secondary: #8e8e8e; --border-color: #dbdbdb; --divider-color: #dbdbdb; --link-color: #0095f6; --hover-bg: #f0f0f0; --tag-bg: #efefef; --profile-icon-bg: #efefef; --modal-overlay: rgba(0, 0, 0, 0.7); }
.dark-mode { --bg-color: #121212; --header-bg: #1e1e1e; --card-bg: #1e1e1e; --text-primary: #f5f5f5; --text-secondary: #a8a8a8; --border-color: #444444; --divider-color: #555555; --link-color: #38bdf8; --hover-bg: #2c2c2c; --tag-bg: #333333; --profile-icon-bg: #ffffff; --modal-overlay: rgba(0, 0, 0, 0.85); }
body { margin: 0; padding: 0; display: block !important; background-color: var(--bg-color); color: var(--text-primary); transition: background-color 0.3s; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale; }
#app { display: block !important; width: 100%; }
.floating-top-btn { position: fixed; bottom: 30px; right: 20px; width: 48px; height: 48px; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 50%; box-shadow: 0 8px 24px rgba(0,0,0,0.12); display: flex; align-items: center; justify-content: center; cursor: pointer; z-index: 3000; }
.top-arrow-icon { display: flex; flex-direction: column; align-items: center; }
.arrow-line { width: 10px; height: 10px; border-top: 2.5px solid var(--link-color); border-left: 2.5px solid var(--link-color); transform: rotate(45deg); margin-bottom: -4.5px; }
.fade-top-enter-active, .fade-top-leave-active { transition: opacity 0.3s; }
.fade-top-enter-from, .fade-top-leave-to { opacity: 0; }

/* 알림 토스트 스타일 */
.noti-toast { position: fixed; bottom: 30px; left: 30px; background: var(--card-bg); border: 1.5px solid var(--border-color); border-radius: 16px; padding: 15px 20px; min-width: 280px; max-width: 350px; display: flex; align-items: center; gap: 15px; box-shadow: 0 15px 40px rgba(0,0,0,0.2); z-index: 9999; cursor: pointer; }
.noti-toast:hover { transform: translateY(-5px); border-color: var(--link-color); }
.t-icon { font-size: 1.8rem; }
.t-content { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.t-label { font-size: 0.7rem; font-weight: 900; color: var(--link-color); text-transform: uppercase; }
.t-text { font-size: 0.85rem; color: var(--text-primary); margin: 0; font-weight: 700; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.t-close { background: none; border: none; font-size: 1.2rem; color: var(--text-muted); cursor: pointer; }
.toast-enter-active, .toast-leave-active { transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.toast-enter-from, .toast-leave-to { transform: translateX(-100%); opacity: 0; }
</style>

<style scoped>
.app-header { background-color: var(--header-bg); position: sticky; top: 0; z-index: 100; height: 60px; width: 100%; display: flex; align-items: center; border-bottom: 1px solid var(--border-color); }
.header-content { max-width: 1200px; width: 100%; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; box-sizing: border-box; }
.header-left-group { display: flex; align-items: center; gap: 15px; }
.theme-toggle-btn { background: none; border: none; font-size: 1.1rem; cursor: pointer; color: var(--text-primary); }
.header-icon-btn { background: none; border: none; font-size: 1.2rem; cursor: pointer; padding: 8px; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: background-color 0.2s; color: var(--text-primary); position: relative; }
.header-icon-btn:hover { background-color: var(--hover-bg); }
.logo-link { text-decoration: none; color: inherit; }
.app-title { font-size: 1.8rem; font-family: 'Grand Hotel', cursive; margin: 0; color: var(--text-primary); }
.main-nav { display: flex; gap: 2rem; align-items: center; flex-grow: 1; justify-content: flex-end; }
.nav-links-group { display: flex; gap: 1.5rem; align-items: center; }
.nav-links { display: flex; gap: 1.2rem; align-items: center; }
.nav-links a { text-decoration: none; color: var(--text-primary); font-size: 0.9rem; font-weight: 500; white-space: nowrap; }
.nav-links a.router-link-active { color: var(--link-color); font-weight: 700; }
.admin-link-highlight { color: #ed4956 !important; font-weight: 800 !important; }
.pc-action-btn { background-color: transparent; border: 1px solid var(--border-color); color: var(--text-secondary); padding: 6px 14px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.pc-action-btn:hover { background-color: var(--hover-bg); color: var(--link-color); border-color: var(--link-color); }
.app-main { width: 100%; padding: 0; }
.content-wrapper { max-width: 1200px; margin: 0 auto; }
.hamburger-btn { display: none; flex-direction: column; gap: 4px; background: none; border: none; cursor: pointer; padding: 10px; }
.bar { width: 20px; height: 2px; background-color: var(--text-primary); border-radius: 2px; }

/* 알림 스타일 */
.header-noti-container { position: relative; }
.noti-icon { font-size: 1.3rem; }
.unread-badge { position: absolute; top: 2px; right: 2px; background: #ed4956; color: white; font-size: 0.6rem; font-weight: 900; min-width: 15px; height: 15px; border-radius: 8px; display: flex; align-items: center; justify-content: center; padding: 0 3px; border: 1.5px solid var(--header-bg); }
.noti-dropdown { position: absolute; top: calc(100% + 10px); right: -10px; width: 300px; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.15); z-index: 1000; overflow: hidden; }
.noti-header { padding: 12px 15px; border-bottom: 1px solid var(--divider-color); display: flex; justify-content: space-between; align-items: center; }
.noti-title { font-weight: 800; font-size: 0.9rem; }
.all-read-btn { background: none; border: none; color: var(--link-color); font-size: 0.7rem; font-weight: 700; cursor: pointer; }
.noti-list { max-height: 350px; overflow-y: auto; }
.noti-item { display: flex; gap: 10px; padding: 12px 15px; cursor: pointer; transition: background 0.2s; border-bottom: 1px solid var(--divider-color); position: relative; }
.noti-item:last-child { border-bottom: none; }
.noti-item:hover { background: var(--hover-bg); }
.noti-item.is-unread { background: rgba(0, 149, 246, 0.03); }
.noti-more-container { padding: 10px; text-align: center; border-top: 1px solid var(--divider-color); }
.noti-more-btn { background: none; border: none; color: var(--link-color); font-size: 0.75rem; font-weight: 700; cursor: pointer; width: 100%; padding: 5px; transition: opacity 0.2s; }
.noti-more-btn:disabled { opacity: 0.5; cursor: default; }
.loading-spinner-small { display: inline-block; width: 12px; height: 12px; border: 2px solid rgba(0, 149, 246, 0.2); border-radius: 50%; border-top-color: var(--link-color); animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.noti-type-icon { font-size: 1.1rem; flex-shrink: 0; }
.noti-body { flex: 1; min-width: 0; }
.noti-content { font-size: 0.8rem; color: var(--text-primary); margin: 0 0 3px; line-height: 1.4; }
.is-unread .noti-content { font-weight: 700; }
.noti-time { font-size: 0.65rem; color: var(--text-secondary); }
.unread-dot { width: 6px; height: 6px; background: #0095f6; border-radius: 50%; position: absolute; right: 10px; top: 50%; transform: translateY(-50%); }
.empty-noti { padding: 30px 15px; text-align: center; color: var(--text-secondary); font-size: 0.8rem; }
.fade-down-enter-active, .fade-down-leave-active { transition: all 0.2s; }
.fade-down-enter-from, .fade-down-leave-to { opacity: 0; transform: translateY(-10px); }
.thin-scrollbar::-webkit-scrollbar { width: 4px; }
.thin-scrollbar::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 10px; }

/* [시니어 조치] 인스타그램 감성 모바일 사이드바 리뉴얼 */
.sidebar-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.4); z-index: 1500; backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px); }
.app-sidebar.insta-style { position: fixed; top: 12px; left: 12px; width: calc(100% - 70px); max-width: 300px; height: calc(100% - 24px); background-color: var(--header-bg); z-index: 1600; border-radius: 28px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); overflow: hidden; border: 1px solid var(--border-color); display: flex; flex-direction: column; }
.sidebar-inner { height: 100%; display: flex; flex-direction: column; }
.sidebar-header { padding: 20px 20px 15px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-color); }
.sidebar-id { font-size: 0.95rem; font-weight: 800; color: var(--text-primary); letter-spacing: -0.3px; }
.sidebar-close-btn { background: none; border: none; padding: 4px; cursor: pointer; color: var(--text-primary); display: flex; align-items: center; opacity: 0.7; }

.sidebar-profile-section { padding: 25px 20px; display: flex; align-items: center; gap: 15px; background: linear-gradient(to bottom, var(--hover-bg), transparent); }
.profile-avatar { width: 52px; height: 52px; border-radius: 50%; background: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%); display: flex; align-items: center; justify-content: center; color: white; font-size: 1.4rem; font-weight: 800; border: 2.5px solid var(--header-bg); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.profile-info { display: flex; flex-direction: column; gap: 1px; }
.profile-nickname { font-size: 1.05rem; font-weight: 800; color: var(--text-primary); }
.profile-sub { font-size: 0.7rem; color: var(--text-secondary); font-weight: 600; opacity: 0.8; }

.sidebar-nav-grid { padding: 12px; display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; flex: 1; overflow-y: auto; }
.nav-item { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px 10px; background: var(--hover-bg); border-radius: 20px; text-decoration: none; color: var(--text-primary); transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); gap: 8px; border: 1px solid transparent; }
.nav-item:active { transform: scale(0.92); background: var(--border-color); }
.nav-item.router-link-active { background: var(--header-bg); border-color: var(--link-color); box-shadow: 0 8px 20px rgba(0, 149, 246, 0.12); }
.nav-icon { font-size: 1.5rem; }
.nav-text { font-size: 0.75rem; font-weight: 700; text-align: center; }

.sidebar-footer { padding: 16px; border-top: 1px solid var(--border-color); background: var(--bg-color); display: flex; flex-direction: column; gap: 4px; }
.footer-action-btn { width: 100%; padding: 10px 12px; border-radius: 12px; border: none; background: transparent; display: flex; align-items: center; gap: 12px; color: var(--text-primary); font-size: 0.85rem; font-weight: 700; cursor: pointer; transition: all 0.2s; text-decoration: none; }
.footer-action-btn:active { background: var(--hover-bg); transform: translateX(5px); }
.footer-icon { font-size: 1.1rem; width: 20px; text-align: center; }
.footer-divider { height: 1px; background: var(--border-color); margin: 6px 8px; opacity: 0.6; }
.footer-action-btn.admin { color: #ed4956; }
.footer-action-btn.logout { color: var(--text-secondary); }
.footer-action-btn.login { background: var(--link-color); color: white; justify-content: center; margin-top: 4px; box-shadow: 0 4px 12px rgba(0, 149, 246, 0.3); }

/* 사이드바 트랜지션 애니메이션 (인스타 스타일의 팝핑 효과) */
.slide-enter-active, .slide-leave-active { transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
.slide-enter-from, .slide-leave-to { transform: translateX(-120%) scale(0.9) rotate(-8deg); opacity: 0; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.4s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.header-right-group { display: none; }

@media (max-width: 768px) {
  .hamburger-btn { display: flex; z-index: 101; }
  .header-left-group { position: absolute; left: 50%; transform: translateX(-50%); z-index: 100; }
  .header-right-group.mobile-only-flex { display: flex; align-items: center; gap: 8px; z-index: 101; margin-left: auto; }
  .desktop-only { display: none !important; }
  .noti-dropdown { right: -10px; width: 260px; max-width: 85vw; border-radius: 16px; }
  .noti-header { padding: 10px 12px; }
  .noti-title { font-size: 0.8rem; }
  .noti-item { padding: 10px 12px; gap: 8px; }
  .noti-type-icon { font-size: 1rem; }
  .noti-content { font-size: 0.75rem; line-height: 1.3; }
  .noti-time { font-size: 0.6rem; }
  .unread-dot { width: 5px; height: 5px; right: 8px; }
}
</style>