# 📱 habiDue PWA & FCM 구축 가이드 (2026-03-29)

본 문서는 habiDue 프로젝트에 적용된 **PWA(Progressive Web App)** 전환 및 **FCM(Firebase Cloud Messaging)** 푸시 알림 시스템의 구축 내역과 향후 운영을 위한 지침을 담고 있습니다.

---

## 🛠 1. 구축 및 구현 내역 (Summary)

### [Frontend] PWA 인프라 완비
- `manifest.json`: 앱 이름, 아이콘, 테마 컬러 정의 (설치 가능 상태 확보).
- `sw.js`: 기본 서비스 워커 등록 및 자산 캐싱 로직 구현.
- `index.html` & `main.js`: PWA 메타 태그 삽입 및 서비스 워커 자동 등록 로직 추가.
- `firebase`: Google FCM SDK 설치 및 백그라운드 메시지 핸들러(`firebase-messaging-sw.js`) 구축.

### [Backend] FCM 푸시 엔진 완비
- `DeviceToken`: 유저별 다중 기기 토큰 관리 엔티티 및 리포지토리 생성.
- `FCMService`: Firebase Admin SDK 기반 실제 푸시 발송 및 유효하지 않은 토큰 자동 정리 로직.
- `NotificationService`: SSE 실시간 알림과 FCM 모바일 푸시 알림 발송 로직 통합.
- `NotificationController`: 프론트엔드 토큰 수집 API (`POST /api/notifications/token`) 추가.

---

## 🔑 2. 사용자 수행 작업 (Critical Config)

시스템 가동을 위해 사용자님께서 **직접 수행하셔야 할 3가지 설정**입니다.

### ① Firebase 콘솔 설정 (Web)
1. **[Firebase Console](https://console.firebase.google.com/)** 접속 -> 프로젝트 생성.
2. **웹 앱 추가**하여 `firebaseConfig` 객체 확보.
3. **클라우드 메시징 -> 웹 설정**에서 '웹 푸시 인증서(VAPID)' 키 생성 및 복사.

### ② 프론트엔드 코드 반영 (Frontend)
- **대상 파일:** `frontend/public/firebase-messaging-sw.js` 및 `frontend/src/utils/fcm.js`
- **작업:** `const firebaseConfig = { ... }` 자리에 위에서 확보한 객체 내용을 복사하여 넣으세요.
- **VAPID 반영:** `fcm.js`의 `vapidKey` 자리에 복사한 VAPID 키 문자열을 넣으세요.

### ③ 백엔드 인증 키 배치 (Backend)
- **대상 위치:** `src/main/resources/firebase-service-account.json`
- **작업:** Firebase 콘솔 [프로젝트 설정 -> 서비스 계정 -> 새 비공개 키 생성]에서 다운로드한 JSON 파일의 이름을 변경하여 해당 위치에 저장하세요.

---

## 🌐 3. 운영 서버 배포 및 설치 관련 가이드

### [설치 및 업데이트]
- **최초 설치:** 사용자가 운영 도메인에 접속 시 브라우저 주소창 우측 또는 팝업을 통해 "홈 화면에 추가"를 눌러 설치합니다.
- **자동 업데이트:** 서버의 코드가 수정되어 배포되면, 사용자가 앱을 지울 필요 없이 브라우저 서비스 워커가 배경에서 자동으로 새 버전을 감지하여 업데이트합니다.

### [운영 환경 체크리스트]
- **HTTPS 필수:** PWA와 FCM은 보안상 `HTTPS` 환경에서만 동작합니다 (로컬 `localhost`는 예외). 운영 서버 배포 시 반드시 SSL 인증서가 적용되어야 합니다.
- **Firebase 승인 도메인:** Firebase 콘솔의 [Authentication -> 설정 -> 승인된 도메인]에 운영 도메인(예: `habidue.com`)을 추가해야 토큰 발급이 정상 작동합니다.
- **아이콘 최적화:** 현재 임시로 `favicon.ico`를 사용 중입니다. 운영 시에는 `public/`에 `192x192` 및 `512x512` 크기의 PNG 아이콘을 추가하고 `manifest.json`을 수정하는 것을 권장합니다.

---

## 🛠 4. 추가 수정 및 트러블슈팅 내역 (Updates)

### [인증 및 보안 관련]
- **401 Unauthorized 해결:** 
  - 프론트엔드(`fcm.js`)에서 인증 토큰이 준비될 때까지 최대 5초간 대기하는 Polling 로직 구현.
  - `JwtAuthenticationFilter`가 `Authorization` 헤더뿐만 아니라 URL 파라미터(`?token=...`)에서도 JWT를 추출할 수 있도록 보강.
  - `SecurityConfig`에서 `/api/notifications/token` 경로를 `permitAll()`로 개방하여 컨트롤러에서 수동 인증을 수행할 수 있도록 조치.
- **URL 정합성 확보:** 백엔드와 프론트엔드 간의 API 경로 불일치(단수/복수)를 `/api/notifications`로 통일.

### [UX 및 서비스 워커 기능 강화]
- **Alert 제거 및 토스트 연동:** 사용자의 작업을 방해하는 브라우저 `alert`을 제거하고 habiDue 고유의 실시간 토스트 알림 시스템과 FCM 포그라운드 메시지를 통합.
- **앱 창 포커스(Focus) 로직:** 알림 배너 클릭 시 현재 열려 있는 PWA 앱 창을 찾아 화면 맨 앞으로 가져오도록 서비스 워커(`firebase-messaging-sw.js`) 로직 고도화.
- **아이콘 및 설치 버튼 활성화:** 루트의 `Habidue.png`를 활용하여 192px/512px 규격을 충족시킴으로써 PC 브라우저의 설치 버튼 활성화 성공.

---

## 💡 관리자용 운영 팁 (Admin Tips)

### 🔄 서비스 워커 강제 업데이트 방법
코드가 수정되었음에도 브라우저에 반영되지 않을 때:
1. `F12(개발자 도구) -> Application -> Service Workers` 접속.
2. `sw.js` 및 `firebase-messaging-sw.js`의 **[Unregister]** 클릭.
3. 페이지 새로고침(`F5`) 하여 최신 워커 재설치 유도.

### 🧹 Redis 데이터 초기화 명령어
- **알림 쿨타임 초기화:** `redis-cli --scan --pattern "noti:cool:*" | xargs redis-cli del`
- **쪽지 발송 제한 초기화:** `redis-cli --scan --pattern "message:count:*" | xargs redis-cli del`
- **전체 데이터 초기화:** `redis-cli flushall`

---
**Final Note:** 이제 habiDue는 단순한 웹사이트를 넘어 유저의 스마트폰과 PC에 상주하는 강력한 '임대주택 비서 앱'으로 진화했습니다.
