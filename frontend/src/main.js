import './assets/main.css';

import { createApp } from 'vue';
import { createPinia } from 'pinia';

import App from './App.vue';
import router from './router';
import axiosInstance from './plugins/axios';
import VCalendar from 'v-calendar';
import 'v-calendar/style.css';

// [시니어 조치] tsParticles 초기화 설정
import Particles from "@tsparticles/vue3";
import { loadSlim } from "@tsparticles/slim";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(VCalendar, {});

// [시니어 조치] 파티클 엔진 등록 (가벼운 slim 버전 사용)
app.use(Particles, {
  init: async engine => {
    await loadSlim(engine);
  },
});

app.config.globalProperties.$axios = axiosInstance;
app.provide('axios', axiosInstance); // provide/inject를 위한 등록

app.mount('#app');
