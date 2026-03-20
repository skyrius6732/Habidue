# HabiDue 프로젝트 3주차 Day 1-2 요약: Vue.js 프론트엔드 & 백엔드 인증 통합

## 🎯 목표 달성 요약
3주차의 첫 목표는 Vue.js 프론트엔드 프로젝트를 생성하고, 백엔드(Spring Boot)와 **구글 OAuth2 소셜 로그인 및 JWT 기반 인증을 완벽하게 연동**하는 것이었습니다. 수많은 시행착오와 디버깅을 거쳐 이 목표를 성공적으로 달성했습니다. 이제 프론트엔드에서 로그인 후 API를 호출하면 Access Token이 자동으로 첨부되고, 만료 시 Refresh Token을 통한 자동 재발급까지 이루어집니다.

## 🛠️ 주요 작업 과정 및 구현 내용

### **1. Vue.js 프론트엔드 프로젝트 생성**
*   **아키텍처:** 백엔드 프로젝트와 동일한 `habiDue` 디렉토리 내부에 `frontend/` 디렉토리를 생성하는 **모놀리식** 구조를 채택했습니다. (`npm create vue@latest frontend`)
*   **주요 스택:** Vue 3 (JavaScript), Vue Router, Pinia (상태 관리), ESLint, Prettier.
*   **HTTP 클라이언트:** `axios` 설치 및 기본 설정 (`npm install axios`).

### **2. 백엔드 인증 엔드포인트 연동 (`HomeView.vue`)**
*   **랜딩 페이지 구현:** `frontend/src/views/HomeView.vue`에 "구글로 시작하기" 버튼을 생성했습니다.
*   **OAuth2 리다이렉트:** 버튼 클릭 시 `window.location.href = 'http://localhost:8081/oauth2/authorization/google';`를 통해 백엔드의 구글 OAuth2 인증 시작점으로 이동하도록 구현했습니다.

### **3. OAuth2 리다이렉트 처리 및 토큰 저장 (`OAuth2RedirectHandler.vue`)**
*   **컴포넌트 생성:** `frontend/src/views/OAuth2RedirectHandler.vue`를 생성했습니다.
*   **토큰 추출 및 저장:** 구글 로그인 성공 후 백엔드로부터 `accessToken`, `refreshToken`을 URL 쿼리 파라미터로 받아와 `localStorage`와 Pinia 스토어(`useAuthStore`)에 저장합니다.
*   **라우팅:** 토큰 저장 후 `router.push('/')`를 통해 홈 페이지로 이동합니다.

### **4. Pinia 스토어를 통한 토큰 전역 관리 (`stores/auth.js`)**
*   **스토어 생성:** `frontend/src/stores/auth.js`에 `useAuthStore`를 정의했습니다.
*   **기능:** `accessToken`과 `refreshToken` 상태를 관리하며, `setTokens`, `clearTokens` 액션을 통해 토큰을 저장/삭제합니다. `isAuthenticated` getter로 로그인 상태를 확인할 수 있습니다.
*   **영속성:** `localStorage`를 사용하여 브라우저를 닫아도 토큰이 유지되도록 했습니다.

### **5. `axios` 인터셉터를 이용한 자동 인증**
*   **인스턴스 생성:** `frontend/src/plugins/axios.js`에 `baseURL`이 `http://localhost:8081`인 `axios` 인스턴스를 생성했습니다.
*   **요청 인터셉터:** 모든 API 요청 전에 `useAuthStore`에서 `accessToken`을 가져와 `Authorization: Bearer [AccessToken]` 헤더에 자동으로 추가하도록 설정했습니다.
*   **응답 인터셉터 (핵심):**
    *   `401 Unauthorized` 응답을 받으면 Access Token 만료로 간주합니다.
    *   `authStore.refreshToken`이 존재하면 백엔드의 `/api/auth/reissue` API를 호출하여 새로운 Access Token을 재발급받습니다. (이때 `Authorization` 헤더는 제거하여 만료된 Access Token이 다시 전달되지 않도록 합니다.)
    *   성공 시 `useAuthStore`에 새 토큰을 저장하고, 원래 실패했던 API 요청을 재시도합니다.
    *   재발급 실패 시 토큰을 모두 삭제하고 로그아웃 처리합니다.

## 🐛 발생했던 주요 이슈 & 해결 과정

이번 3주차 Day 1-2 과정에서 수많은 난관에 부딪혔지만, 다음 이슈들을 해결하며 시스템을 완성했습니다.

1.  **DB Password `NOT NULL` 오류:**
    *   **이슈:** `User` 엔티티의 `password` 컬럼이 `NOT NULL`인데, 소셜 로그인 사용자는 비밀번호가 없어 DB 저장 시 오류 발생.
    *   **해결:**
        *   `User.java`의 `password` 컬럼을 `nullable = true`로 변경.
        *   `CustomOAuth2UserService`에서 신규 유저 생성 시 `java.util.UUID.randomUUID().toString()`으로 임의의 비밀번호를 생성하여 저장.
        *   `docker exec` 명령어로 MySQL `users` 테이블의 `password` 컬럼을 `NULL` 허용으로 변경 (`ALTER TABLE users MODIFY password VARCHAR(255) NULL;`).

2.  **JWT 인증 실패 (HTML 페이지 반환):**
    *   **이슈:** 인증된 사용자가 `/api/auth/test/user`에 접근했음에도 구글 로그인 페이지의 HTML이 응답으로 옴.
    *   **원인:** `SecurityConfig`에 `ROLE_USER` 권한 설정이 누락되어, 스프링 시큐리티가 권한 없는 접근으로 간주하고 로그인 페이지로 리다이렉트.
    *   **해결:** `SecurityConfig`에 `/api/auth/test/**` 경로에 `hasRole("USER")` 권한 설정 추가. `UserPrincipal` 도입 및 `JwtTokenProvider`와 `AuthController` 수정.

3.  **WSL2 `localhost` vs `172.x.x.x` IP 문제:**
    *   **이슈:** Windows 브라우저에서 WSL2 Docker MySQL(`172.x.x.x:3306`)로 `localhost:3306`을 통해 접속하려 할 때, Windows에 설치된 다른 DB 서비스(MariaDB)가 3306 포트를 점유하여 `GSS-API authentication exception` 발생.
    *   **해결:**
        *   MySQL 컨테이너를 재생성하며 `default-authentication-plugin=mysql_native_password` 옵션을 명확히 추가.
        *   DB `root` 유저가 `localhost` 외 `%`에서도 접근 가능하도록 권한 부여 및 인증 플러그인 `mysql_native_password`로 통일.
        *   프론트엔드 `HomeView.vue` 및 백엔드 `OAuth2SuccessHandler.java`에서 백엔드 URL을 **`http://localhost:8081`**로 사용하도록 통일하여 Windows-WSL2 포트 포워딩의 이점 활용.

4.  **`axios` `Authorization` 헤더 누락/불일치:**
    *   **이슈:** 프론트엔드에서 API 호출 시 `Authorization` 헤더에 토큰이 제대로 담기지 않거나, 백엔드에서 `유효한 JWT 토큰이 없습니다` 로그 발생.
    *   **원인:**
        *   `AboutView.vue`에서 `axios` 인스턴스 주입 방식 (`inject` vs `globalProperties.$axios`) 문제.
        *   `axios.js` 인터셉터가 Pinia `useAuthStore()`를 호출하는 시점 문제.
        *   `AuthController`의 `@RequestMapping` 경로가 `axios.js` `baseURL`과 충돌.
    *   **해결:**
        *   `AboutView.vue`에서 `getCurrentInstance().proxy.$axios`를 통해 `globalProperties.$axios`를 명확히 사용.
        *   `axios.js` 인터셉터 내에서 `useAuthStore()`를 호출하여 Pinia 스토어 초기화 시점 문제 해결.
        *   `AuthController`의 `@RequestMapping`을 `/api/auth`로 변경하고, 프론트엔드 호출 경로도 `'/api/auth/test/user'`로 수정하여 일관성 유지.

5.  **CORS `Pre-flight` 오류:**
    *   **이슈:** `POST` 요청 시 브라우저 콘솔에 `CORS` 오류 발생.
    *   **원인:** 백엔드(`localhost:8081`)가 프론트엔드(`localhost:5173`)로부터 오는 `OPTIONS` 요청을 제대로 처리하지 못함.
    *   **해결:** `SecurityConfig`의 `CorsConfigurationSource`에 `allowedMethods`에 `OPTIONS`를 포함하고, `allowedHeaders`에 `Authorization-Refresh`를 추가. `exposedHeaders`에 `Authorization`을 추가하여 프론트엔드가 재발급된 토큰을 읽을 수 있도록 허용.

6.  **순환 참조 오류 (`Circular Reference`):**
    *   **이슈:** `JwtAuthenticationEntryPoint`와 `SecurityConfig` 간의 `ObjectMapper` 주입으로 인한 순환 참조 발생.
    *   **해결:** `JwtAuthenticationEntryPoint` 내에서 `ObjectMapper`를 `@Autowired` 대신 `new ObjectMapper()`로 직접 생성하여 의존성 고리 끊음.

7.  **`LocalDateTime` 직렬화 오류 (`InvalidDefinitionException`):**
    *   **이슈:** `ApiResponse` 객체의 `timestamp` 필드(`LocalDateTime`)를 JSON으로 변환할 때 오류 발생.
    *   **원인:** `JwtAuthenticationEntryPoint` 내에서 직접 생성한 `ObjectMapper`가 Java 8 날짜/시간 타입을 기본적으로 인식하지 못함.
    *   **해결:** `build.gradle`에 `jackson-datatype-jsr310` 의존성을 추가하고, `JwtAuthenticationEntryPoint`에서 `ObjectMapper`를 생성할 때 `objectMapper.registerModule(new JavaTimeModule());`을 통해 `JavaTimeModule`을 명시적으로 등록.

8.  **Refresh Token 재발급 무한 루프 & `ExpiredJwtException` 상세 처리:**
    *   **이슈:** Access Token 만료 시 `axios` 인터셉터가 `reissue` API를 호출했지만, `reissue` API도 만료된 토큰으로 인해 `401 Unauthorized`를 받아 무한 루프 발생.
    *   **원인:**
        *   `JwtAuthenticationFilter`가 `reissue` API 경로도 Access Token으로 필터링하고 있었음.
        *   `reissue` API 호출 시 `axios` 인터셉터가 `Authorization` 헤더를 제거하지 못함.
    *   **해결:**
        *   `SecurityConfig`에 `/api/auth/reissue` 경로를 `permitAll()`로 설정하여 JWT 필터의 검증 대상에서 제외.
        *   `axios.js` 인터셉터에서 `reissue` API 호출 시 `delete config.headers.Authorization;`을 사용하여 `Authorization` 헤더를 명시적으로 제거.

## 🎉 3주차 Day 1-2 목표 완벽 달성!

이로써 HabiDue 프로젝트의 Vue.js 프론트엔드와 Spring Boot 백엔드 간 **구글 OAuth2 및 JWT 기반 인증 연동이 완벽하게 구축**되었습니다. Access Token 만료 시 Refresh Token을 통한 자동 재발급 로직까지 성공적으로 테스트 완료했습니다.

---

## 🚀 다음 단계 (3주차 Day 3-4): 핵심 화면 구현 (리스트/캘린더 뷰)
이제 프론트엔드와 백엔드 연동의 가장 큰 산을 넘었으니, 다음은 사용자가 직접 볼 수 있는 핵심 화면을 구현하는 것입니다. 구글 로그인 후 메인 페이지에서 공고 리스트를 보여주고, 사용자의 키워드와 관심 공고를 관리할 수 있는 UI를 만들어나가겠습니다.
