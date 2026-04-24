// wedding 기능 라우트 모음 - 이 파일만 삭제하면 라우트 전체 제거됨

export const weddingPublicRoutes = [
  {
    path: '/wedding/:slug',
    name: 'weddingView',
    component: () => import('./views/WeddingView.vue'),
  }
]

export const weddingAdminRoutes = [
  {
    path: 'wedding',
    name: 'adminWedding',
    component: () => import('./views/WeddingAdminListView.vue'),
  },
  {
    path: 'wedding/new',
    name: 'adminWeddingNew',
    component: () => import('./views/WeddingAdminFormView.vue'),
  },
  {
    path: 'wedding/:id/edit',
    name: 'adminWeddingEdit',
    component: () => import('./views/WeddingAdminFormView.vue'),
  },
  {
    path: 'wedding/:id/rsvp',
    name: 'adminWeddingRsvp',
    component: () => import('./views/WeddingAdminRsvpView.vue'),
  }
]
