import './assets/main.css';

import { createApp } from 'vue';
import { createPinia } from 'pinia';

import App from './App.vue';
import router from './router';
import axiosInstance from './plugins/axios';
import VCalendar from 'v-calendar';
import 'v-calendar/style.css';
import { getAnalytics, logEvent } from 'firebase/analytics';

// [시니어 조치] tsParticles 초기화 설정
import Particles from "@tsparticles/vue3";
import { loadSlim } from "@tsparticles/slim";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(VCalendar, {});

// [시니어 조치] Google Analytics 페이지 뷰 추적
const analytics = getAnalytics();
router.afterEach((to) => {
  logEvent(analytics, 'page_view', {
    page_path: to.fullPath,
    page_title: to.name || to.path,
    page_location: window.location.href
  });
});

// [시니어 조치] 파티클 엔진 등록 (가벼운 slim 버전 사용)
app.use(Particles, {
  init: async engine => {
    await loadSlim(engine);
  },
});

app.config.globalProperties.$axios = axiosInstance;
app.provide('axios', axiosInstance); // provide/inject를 위한 등록

app.mount('#app');

// [PWA 조치] 서비스 워커 및 FCM 등록
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    // 1. 기본 PWA 워커 등록
    navigator.serviceWorker.register('/sw.js')
      .then(reg => console.log('PWA ServiceWorker registered:', reg.scope))
      .catch(err => console.log('PWA ServiceWorker failed:', err));

    // 2. FCM 전용 워커 등록 (상단 알림 필수)
    navigator.serviceWorker.register('/firebase-messaging-sw.js')
      .then(reg => console.log('FCM ServiceWorker registered:', reg.scope))
      .catch(err => console.log('FCM ServiceWorker failed:', err));
  });
}

