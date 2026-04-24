<template>
  <div class="admin-layout" :class="{ 'sidebar-open': isSidebarOpen }">
    <!-- 모바일 전용 오버레이 -->
    <Transition name="fade">
      <div v-if="isSidebarOpen" class="mobile-overlay" @click="isSidebarOpen = false"></div>
    </Transition>

    <!-- 사이드바 -->
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <h2 class="admin-logo">Admin Portal</h2>
        <button class="mobile-close-btn" @click="isSidebarOpen = false">&times;</button>
      </div>
      <nav class="sidebar-nav scrollable">
        <RouterLink to="/admin/dashboard" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">📊</span> <span class="label">대시보드</span>
        </RouterLink>
        <RouterLink to="/admin/users" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">👥</span> <span class="label">사용자 관리</span>
        </RouterLink>
        <RouterLink to="/admin/notices" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">🏠</span> <span class="label">공고 관리</span>
        </RouterLink>
        <RouterLink to="/admin/community" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">💬</span> <span class="label">커뮤니티 관리</span>
        </RouterLink>
        <RouterLink to="/admin/inquiries" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">📫</span> <span class="label">고객센터 문의 관리</span>
        </RouterLink>
        <RouterLink to="/admin/simulation" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">🔬</span> <span class="label">보상 시뮬레이션</span>
        </RouterLink>
        <RouterLink to="/admin/tags" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">🏷️</span> <span class="label">태그 관리</span>
        </RouterLink>
        <RouterLink to="/admin/metadata" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">🗂️</span> <span class="label">메타데이터 관리</span>
        </RouterLink>
        <RouterLink to="/admin/badges" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">🎖️</span> <span class="label">배지 마스터리 관리</span>
        </RouterLink>
        <RouterLink to="/admin/about" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">📢</span> <span class="label">공지 및 패치 관리</span>
        </RouterLink>
        <RouterLink to="/admin/wedding" class="nav-item" @click="isSidebarOpen = false">
          <span class="icon">💍</span> <span class="label">모바일 청첩장</span>
        </RouterLink>
      </nav>
      <div class="sidebar-footer">
        <RouterLink to="/notices" class="back-to-site">서비스로 돌아가기</RouterLink>
      </div>
    </aside>

    <!-- 메인 컨텐츠 영역 -->
    <main class="admin-main">
      <header class="admin-top-bar">
        <div class="top-left">
          <button class="menu-toggle-btn" @click="isSidebarOpen = true">☰</button>
          <div class="breadcrumb">{{ currentMenuName }}</div>
        </div>
        <div class="admin-profile">
          <span class="admin-badge">ADMIN</span>
          <span class="admin-name desktop-only">{{ authStore.user?.username }}님</span>
        </div>
      </header>
      <section class="admin-content" :class="{ 'content-fullscreen': isSimulation }">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const isSidebarOpen = ref(false)

const isSimulation = computed(() => route.path.includes('simulation'))

const currentMenuName = computed(() => {
  if (route.path.includes('dashboard')) return '대시보드'
  if (route.path.includes('users')) return '사용자 관리'
  if (route.path.includes('notices')) return '공고 관리'
  if (route.path.includes('community')) return '커뮤니티 관리'
  if (route.path.includes('inquiries')) return '고객센터 문의 관리'
  if (route.path.includes('simulation')) return '보상 시뮬레이션'
  if (route.path.includes('tags')) return '태그 관리'
  if (route.path.includes('metadata')) return '메타데이터 관리'
  if (route.path.includes('badges')) return '배지 마스터리 관리'
  if (route.path.includes('about')) return '공지 및 패치 관리'
  return '관리자 센터'
})
</script>

<style scoped>
.admin-layout { 
  display: flex; 
  min-height: 100vh; 
  background-color: var(--bg-color); 
  color: var(--text-primary); 
  position: relative; 
  transition: all 0.3s;
}

/* 사이드바 (테마 연동) */
.admin-sidebar { 
  width: 240px; 
  background-color: var(--header-bg); 
  color: var(--text-primary); 
  display: flex; 
  flex-direction: column;
  transition: transform 0.3s, background-color 0.3s; 
  z-index: 3000; 
  position: sticky; 
  top: 0; 
  height: 100vh;
  flex-shrink: 0;
  border-right: 1px solid var(--border-color);
}

.sidebar-header { 
  height: 60px;
  padding: 0 15px; 
  border-bottom: 1px solid var(--border-color); 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  box-sizing: border-box;
  flex-shrink: 0;
}
.admin-logo { font-size: 1.2rem; font-weight: 800; margin: 0; color: var(--link-color); letter-spacing: -0.5px; }
.mobile-close-btn { display: none; background: none; border: none; color: var(--text-primary); font-size: 1.5rem; cursor: pointer; padding: 0; line-height: 1; }

.sidebar-nav { flex: 1; padding: 5px 0; overflow-y: auto; }
.nav-item { display: flex; align-items: center; gap: 10px; padding: 6px 20px; color: var(--text-secondary); text-decoration: none; transition: all 0.2s; font-weight: 600; font-size: 0.9rem; }
.nav-item:hover { background-color: var(--hover-bg); color: var(--text-primary); }
.nav-item.router-link-active { background-color: var(--hover-bg); color: var(--link-color); border-left: 4px solid var(--link-color); padding-left: 16px; }
.nav-item .icon { font-size: 1.1rem; min-width: 20px; text-align: center; }

.sidebar-footer { padding: 10px 15px; border-top: 1px solid var(--border-color); flex-shrink: 0; }
.back-to-site { color: var(--text-secondary); font-size: 0.8rem; text-decoration: none; font-weight: 500; }
.back-to-site:hover { color: var(--link-color); text-decoration: underline; }

/* 메인 영역 */
.admin-main { 
  flex: 1; 
  display: flex; 
  flex-direction: column; 
  min-width: 0; 
  min-height: 100vh;
  position: relative;
  z-index: 1; /* 사이드바보다 확실히 낮게 설정 */
}
.admin-top-bar { 
  height: 60px; 
  background: var(--header-bg); 
  border-bottom: 1px solid var(--border-color); 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  padding: 0 25px; 
  flex-shrink: 0; 
  transition: background-color 0.3s; 
  position: sticky; 
  top: 0; 
  z-index: 100; /* admin-main 내부에서의 우선순위 */
}

.top-left { display: flex; align-items: center; gap: 12px; }
.menu-toggle-btn { display: none; background: var(--hover-bg); border: none; font-size: 1.2rem; cursor: pointer; color: var(--text-primary); width: 36px; height: 36px; border-radius: 8px; align-items: center; justify-content: center; }
.breadcrumb { font-weight: 700; font-size: 1rem; color: var(--text-primary); }

.admin-profile { display: flex; align-items: center; gap: 10px; }
.admin-badge { background: #ed4956; color: white; font-size: 0.65rem; font-weight: 800; padding: 2px 6px; border-radius: 4px; }
.admin-name { font-weight: 600; font-size: 0.85rem; color: var(--text-primary); }

.admin-content { flex: 1; padding: 15px; display: flex; flex-direction: column; min-height: 0; }
.admin-content.content-fullscreen { padding: 0; overflow: hidden; }

/* 스크롤바 스타일 통일 */
.scrollable { scrollbar-width: thin; }
.scrollable::-webkit-scrollbar { width: 4px; }
.scrollable::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 10px; }

/* 애니메이션 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 모바일 대응 (992px 이하) */
@media (max-width: 992px) {
  .admin-sidebar { 
    position: fixed; top: 0; left: 0; bottom: 0; 
    transform: translateX(-100%);
    box-shadow: 10px 0 30px rgba(0,0,0,0.1);
    height: 100vh;
    z-index: 9999 !important; /* 압도적인 우선순위 부여 */
  }
  .admin-layout.sidebar-open .admin-sidebar { transform: translateX(0); }
  .mobile-overlay { 
    position: fixed; top: 0; left: 0; right: 0; bottom: 0; 
    background: rgba(0,0,0,0.5); 
    z-index: 9000 !important; /* 상단 바보다 높게, 사이드바보다는 낮게 */
    backdrop-filter: blur(2px); 
  }
  .mobile-close-btn { display: block; }
  .menu-toggle-btn { display: flex; }
  .admin-top-bar { padding: 0 15px; height: 56px; z-index: 100; }
  .admin-content { padding: 20px 15px; overflow-y: auto; }
  .admin-content.content-fullscreen { padding: 0; overflow: visible; }
  .desktop-only { display: none; }
  .admin-main { width: 100%; min-height: 100vh; z-index: 1; }
}
</style>
