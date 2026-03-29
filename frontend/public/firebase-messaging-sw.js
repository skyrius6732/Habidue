// Firebase Background Messaging Service Worker
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-messaging-compat.js');

const firebaseConfig = {
  apiKey: "AIzaSyArB7quWh8fkdV1JYpQbNyfifIoSmTKROo",
  authDomain: "habidue-f1420.firebaseapp.com",
  projectId: "habidue-f1420",
  storageBucket: "habidue-f1420.firebasestorage.app",
  messagingSenderId: "1001502110032",
  appId: "1:1001502110032:web:1734fac32c837f05197c55",
  measurementId: "G-XVY5H34QK8"
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

// 백그라운드 메시지 수신
messaging.onBackgroundMessage((payload) => {
  // [시니어 조치] 백엔드에서 notification 객체 대신 data 객체를 보내므로 이를 수신하도록 수정
  const data = payload.data || {};
  const notificationTitle = data.title || 'habiDue 알림';
  const notificationOptions = {
    body: data.body || '',
    icon: '/icon.png',
    badge: '/favicon.ico',
    tag: 'habidue-notification', // 같은 태그의 알림이 오면 기존 알림을 대체함 (도배 방지)
    renotify: true, // 새로운 알림이 오면 다시 진동/소리 발생
    data: { url: data.click_action || '/' }
  };

  return self.registration.showNotification(notificationTitle, notificationOptions);
});

// [최종 강화] 알림 클릭 시 앱 창 활성화 로직
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  // [시니어 조치] 알림 데이터에 포함된 URL이 있으면 해당 URL로, 없으면 메인으로 이동
  const relativeUrl = (event.notification.data && event.notification.data.url) ? event.notification.data.url : '/';
  const targetUrl = self.location.origin + relativeUrl;

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then((windowClients) => {
        // 1. 이미 열려 있는 창이 있는지 확인
        for (let i = 0; i < windowClients.length; i++) {
          const client = windowClients[i];
          // 우리 사이트 주소가 포함된 창이 있다면 포커스하고 페이지 이동
          if (client.url.indexOf(self.location.origin) !== -1 && 'focus' in client) {
            console.log('[SW] Found matching window, navigating to:', targetUrl);
            if (client.url !== targetUrl) {
              client.navigate(targetUrl);
            }
            return client.focus();
          }
        }
        
        // 2. 열린 창이 아예 없다면 새 창으로 타겟 URL 열기
        if (clients.openWindow) {
          return clients.openWindow(targetUrl);
        }
      })
  );
});
