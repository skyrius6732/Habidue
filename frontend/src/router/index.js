import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import HomeView from '../views/HomeView.vue';
import OAuth2RedirectHandler from '../views/OAuth2RedirectHandler.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/oauth2/redirect',
      name: 'oauth2Redirect',
      component: OAuth2RedirectHandler,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/notices',
      name: 'notices',
      component: () => import('../views/NoticeListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/interests',
      name: 'interests',
      component: () => import('../views/MyInterestsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/keywords',
      name: 'keywords',
      component: () => import('../views/MyKeywordsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/calendar',
      name: 'calendar',
      component: () => import('../views/CalendarView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/messages',
      name: 'messages',
      redirect: '/keywords?tab=messages',
      meta: { requiresAuth: true }
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('../views/SearchView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/board/:noticeId?',
      name: 'board',
      component: () => import('../views/BoardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/board/post/:postId',
      name: 'postDetail',
      component: () => import('../views/PostDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/board/write',
      name: 'postWrite',
      component: () => import('../views/PostWriteView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/board/post/edit/:postId',
      name: 'postEdit',
      component: () => import('../views/PostWriteView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/withdrawal-success',
      name: 'withdrawalSuccess',
      component: () => import('../views/WithdrawalSuccessView.vue'),
    },
    {
      path: '/blocked',
      name: 'blocked',
      component: () => import('../views/BlockedView.vue')
    },
    {
      path: '/login-error',
      name: 'loginError',
      component: { template: '<div class="error-page"><h1>로그인 오류 발생!</h1><p>다시 시도해주세요.</p></div>' }
    },
    // --- [관리자 전용 라우트] ---
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayoutView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          redirect: { name: 'adminUsers' }
        },
        {
          path: 'users',
          name: 'adminUsers',
          component: () => import('../views/admin/AdminUserView.vue')
        },
        {
          path: 'dashboard',
          name: 'adminDashboard',
          component: () => import('../views/admin/AdminDashboardView.vue')
        },
        {
          path: 'notices',
          name: 'adminNotices',
          component: () => import('../views/admin/AdminNoticeView.vue')
        },
        {
          path: 'community',
          name: 'adminCommunity',
          component: () => import('../views/admin/AdminCommunityView.vue')
        },
        {
          path: 'tags',
          name: 'adminTags',
          component: () => import('../views/admin/AdminTagView.vue')
        },
        {
          path: 'metadata',
          name: 'adminMetadata',
          component: () => import('../views/admin/AdminMetadataView.vue')
        },
        {
          path: 'badges',
          name: 'adminBadges',
          component: () => import('../views/admin/AdminBadgeView.vue')
        },
        {
          path: 'about',
          name: 'adminAbout',
          component: () => import('../views/admin/AboutAdminView.vue')
        }
      ]
    },
    // 존재하지 않는 모든 경로는 404 페이지로 안내
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('../views/NotFoundView.vue')
    }
  ],
});

// 네비게이션 가드: 로그인 여부 체크 및 리다이렉트
router.beforeEach((to, from) => {
  const authStore = useAuthStore();
  
  // [시니어 조치] 토큰 존재 여부를 더 엄격하게 체크하여 무한 루프 방지
  const token = localStorage.getItem('accessToken');
  const isAuthenticated = !!token;
  const userRole = localStorage.getItem('userRole'); 
  
  // 1. 이미 home으로 가고 있는 중이면 더 이상의 리다이렉트 금지
  if (to.name === 'home') {
    if (isAuthenticated) {
      return { name: 'notices' };
    }
    return;
  }

  // 2. 인증이 필요한 페이지인데 토큰이 없는 경우
  if (to.meta.requiresAuth && !isAuthenticated) {
    // alert('로그인이 필요한 서비스입니다.');
    return { name: 'home' };
  } 
  
  // 3. 관리자 권한 체크
  if (to.meta.requiresAdmin && userRole !== 'ADMIN') {
    alert('접근 권한이 없습니다. 관리자 전용 페이지입니다.');
    return { name: 'notices' };
  }

  // 4. 그 외 정상적인 이동
  return;
});

export default router;
