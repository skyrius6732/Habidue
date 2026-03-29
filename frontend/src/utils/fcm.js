// src/utils/fcm.js
import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage } from 'firebase/messaging';
import axios from '@/plugins/axios';
import { useAuthStore } from '@/stores/auth';

// [사용자 조치] Firebase 콘솔에서 발급받은 firebaseConfig를 여기에 복사해 넣어주세요.
const firebaseConfig = {
  apiKey: "AIzaSyArB7quWh8fkdV1JYpQbNyfifIoSmTKROo",
  authDomain: "habidue-f1420.firebaseapp.com",
  projectId: "habidue-f1420",
  storageBucket: "habidue-f1420.firebasestorage.app",
  messagingSenderId: "1001502110032",
  appId: "1:1001502110032:web:1734fac32c837f05197c55",
  measurementId: "G-XVY5H34QK8"
};

const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);

// 푸시 알림 권한 요청 및 토큰 가져오기
export const requestFcmToken = async () => {
  // 브라우저가 알림을 지원하는지 먼저 확인
  if (!('Notification' in window)) {
    console.log('This browser does not support notifications.');
    return;
  }

  // 이미 허용된 상태라면 바로 토큰 가져오기 시도
  if (Notification.permission === 'granted') {
    return await fetchToken();
  }

  // 사용자의 첫 클릭 시점에 알림 권한을 요청하도록 이벤트 리스너 등록 (브라우저 차단 회피용)
  if (Notification.permission === 'default') {
    const handleFirstInteraction = async () => {
      console.log('User interacted, requesting notification permission...');
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        await fetchToken();
      }
      window.removeEventListener('click', handleFirstInteraction);
    };
    window.addEventListener('click', handleFirstInteraction);
  }
};

// 실제 토큰 발급 로직 분리
const fetchToken = async () => {
  try {
    // [사용자 조치] Firebase 웹 푸시 인증서(VAPID) 키를 여기에 넣어주세요.
    const token = await getToken(messaging, { 
      vapidKey: 'BNC0gEoDPi5Le9f0HlFv4JbvItomYSdxCO7itfsijP2OMMnJPFgYrigoguZkLWJNk7UPz1sm0ej2ouxRObAL5MA' 
    });
    
    if (token) {
      console.log('FCM Token generated: ', token);
      await saveTokenToBackend(token);
      return token;
    }
  } catch (error) {
    console.error('An error occurred while retrieving token. ', error);
  }
};

// 서버(Spring Boot)에 토큰 저장
const saveTokenToBackend = async (token) => {
  try {
    const authStore = useAuthStore();
    
    // [시니어 조치] 토큰이 생길 때까지 최대 5초간 대기
    let retryCount = 0;
    while (!authStore.accessToken && retryCount < 10) {
      await new Promise(resolve => setTimeout(resolve, 500));
      retryCount++;
    }

    if (!authStore.accessToken) {
      console.warn('[FCM] Authentication timed out.');
      return;
    }

    // [시니어 조치] axios 인터셉터 및 헤더 이슈를 배제하기 위해 SSE와 동일한 URL 파라미터 방식 사용
    console.log('[FCM] Attempting to save token with fetch (URL param)...');
    const url = `/api/notifications/token?token=${encodeURIComponent(authStore.accessToken)}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ token })
    });

    if (response.ok) {
      console.log('FCM Token saved to backend successfully (via fetch).');
    } else {
      console.error('Failed to save FCM Token. Status:', response.status);
      const errorText = await response.text();
      console.error('Error detail:', errorText);
    }
  } catch (error) {
    console.error('Failed to save FCM Token to backend: ', error);
  }
};

// 포그라운드 메시지 수신 (앱을 켜놓고 있을 때)
export const onForegroundMessage = (callback) => {
  onMessage(messaging, (payload) => {
    console.log('[FCM] Message received in foreground: ', payload);
    if (callback && typeof callback === 'function') {
      // alert 대신 콜백(토스트 등) 호출
      callback(payload);
    }
  });
};
