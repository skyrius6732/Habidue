# HabiDue 인증 흐름 심층 분석: Google OAuth2, JWT, Refresh Token & Axios 인터셉터

이 문서는 HabiDue 프로젝트의 핵심인 **Google OAuth2 기반 로그인부터 JWT(Access/Refresh Token) 발행, 그리고 API 인증 및 만료된 토큰 재발급 과정까지**의 전체 흐름을 백엔드(Java)와 프론트엔드(Vue.js/JavaScript)의 주요 컴포넌트들을 중심으로 상세히 설명합니다.

---

## 🚀 0. 전체 인증 흐름 개요

사용자가 "구글로 로그인" 버튼을 누르면, 구글 서버가 사용자를 인증해 줍니다. 인증이 완료되면 우리 백엔드는 사용자 정보를 바탕으로 **Access Token(단기 통행증)**과 **Refresh Token(장기 갱신권)**을 발행합니다. Access Token은 API 요청에 사용되고, 만료되면 Refresh Token으로 새로운 Access Token을 재발급받습니다. 이 모든 과정은 `axios` 인터셉터를 통해 프론트엔드에서 자동으로 처리됩니다.

---

## 🏃 1. Google 로그인 (최초 인증) 상세 흐름

**목표:** 사용자가 구글로 로그인하고, 백엔드가 이를 처리하여 JWT 토큰을 발행한 후 프론트엔드로 전달합니다.

1.  **사용자: "구글로 로그인" 클릭**
    *   **위치:** `frontend/src/views/HomeView.vue`
    *   **주요 코드:**
        ```vue
        // frontend/src/views/HomeView.vue
        const loginWithGoogle = () => {
          window.location.href = 'http://localhost:8081/oauth2/authorization/google';
        };
        ```
    *   **설명:** 사용자가 버튼을 클릭하면, 브라우저가 백엔드의 Google OAuth2 인증 시작 엔드포인트로 리다이렉트됩니다.

2.  **백엔드: OAuth2 요청 가로채기 & 구글로 리다이렉트**
    *   **위치:** `backend/src/main/java/com/habidue/app/config/SecurityConfig.java`
    *   **주요 코드:**
        ```java
        // backend/src/main/java/com/habidue/app/config/SecurityConfig.java
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)
            )
            .successHandler(oAuth2SuccessHandler)
        );
        ```
    *   **설명:** Spring Security의 `oauth2Login()` 설정이 `http://localhost:8081/oauth2/authorization/google` 요청을 가로챕니다. `application.yml`에 설정된 `client-id` 등을 사용하여 사용자를 Google 로그인/동의 페이지로 리다이렉트합니다.

3.  **구글: 사용자 인증 및 백엔드로 리다이렉트**
    *   **동작:** 사용자가 구글에서 로그인하면, 구글은 HabiDue 서비스에 인증이 성공했음을 알리는 임시 `code`를 발급하고, 브라우저를 백엔드의 **`http://localhost:8081/login/oauth2/code/google`**로 리다이렉트합니다.

4.  **백엔드: 사용자 정보 처리 (DB 저장/업데이트)**
    *   **위치:** `backend/src/main/java/com/habidue/app/service/CustomOAuth2UserService.java`
    *   **주요 클래스/메소드:** `CustomOAuth2UserService.loadUser()`
    *   **설명:** Spring Security는 구글에서 받은 `code`를 이용해 구글 API로부터 사용자 정보(`email`, `name`, `sub`(고유 ID) 등)를 가져옵니다. `loadUser()` 메소드는 이 정보를 바탕으로:
        *   `userRepository.findByProviderAndProviderId()`를 통해 DB에 이미 등록된 사용자인지 확인합니다.
        *   **신규 사용자:** `User` 객체를 생성하고 `email`, `provider`, `providerId`, `ROLE_USER` 권한, 그리고 임의의 `password`를 설정하여 `userRepository.save()`로 DB에 저장합니다. (자동 회원가입)
        *   **기존 사용자:** 기존 `User` 정보를 그대로 사용합니다.
        *   최종적으로 `UserPrincipal.create()`를 통해 인증 객체를 생성하여 Spring Security 컨텍스트에 담습니다.

5.  **백엔드: JWT 토큰 생성, Redis 저장 & 프론트엔드로 리다이렉트**
    *   **위치:** `backend/src/main/java/com/habidue/app/config/oauth/OAuth2SuccessHandler.java`
    *   **주요 클래스/메소드:** `OAuth2SuccessHandler.onAuthenticationSuccess()`
    *   **설명:**
        *   `CustomOAuth2UserService`에서 처리된 인증 정보(`Authentication` 객체, 여기에 `UserPrincipal` 포함)를 전달받습니다.
        *   `JwtTokenProvider.createAccessToken(authentication)`와 `JwtTokenProvider.createRefreshToken(authentication)`를 호출하여 Access Token과 Refresh Token을 생성합니다.
        *   **`RedisTemplate`** (`backend/src/main/java/com/habidue/app/config/oauth/OAuth2SuccessHandler.java` 내에서 주입): 생성된 Refresh Token을 사용자의 `email`을 키로 Redis에 저장하고, Refresh Token의 만료 시간과 동일한 TTL(Time To Live)을 설정합니다.
        *   Access Token과 Refresh Token을 URL 쿼리 파라미터에 담아 프론트엔드 (`http://localhost:5173/oauth2/redirect`)로 리다이렉트합니다.

6.  **프론트엔드: 토큰 수신 및 저장**
    *   **위치:** `frontend/src/views/OAuth2RedirectHandler.vue`, `frontend/src/stores/auth.js`
    *   **주요 클래스/메소드:** `OAuth2RedirectHandler.onMounted()`, `useAuthStore.setTokens()`
    *   **설명:**
        *   브라우저가 `http://localhost:5173/oauth2/redirect`로 이동하면 `OAuth2RedirectHandler.vue` 컴포넌트가 로드됩니다.
        *   `onMounted` 훅에서 URL 쿼리 파라미터로부터 `accessToken`과 `refreshToken`을 추출합니다.
        *   `useAuthStore().setTokens()`를 호출하여 이 토큰들을 `localStorage`에 저장하고, Pinia 스토어에도 상태를 업데이트합니다.
        *   토큰 저장 후 `router.push('/')`를 통해 사용자에게 홈 페이지를 보여줍니다.

---

## 🔒 2. API 호출 (Access Token 사용) 상세 흐름

**목표:** 프론트엔드가 백엔드 API를 호출할 때 Access Token을 자동으로 첨부하여 인증된 사용자만 접근하도록 합니다.

1.  **사용자: "인증 API 테스트" 버튼 클릭**
    *   **위치:** `frontend/src/views/AboutView.vue`
    *   **주요 코드:**
        ```vue
        // frontend/src/views/AboutView.vue
        const testAuthApi = async () => {
          // ...
          const response = await axios.get('/api/auth/test/user');
          // ...
        };
        ```
    *   **설명:** 사용자가 버튼을 클릭하면 `axios.get('/api/auth/test/user')` 호출을 통해 백엔드 API에 요청을 보냅니다.

2.  **프론트엔드: 요청에 Access Token 자동 첨부**
    *   **위치:** `frontend/src/plugins/axios.js`
    *   **주요 클래스/메소드:** `instance.interceptors.request.use()`
    *   **설명:**
        *   `axios`의 요청 인터셉터가 `instance.get()` 호출을 가로챕니다.
        *   `useAuthStore().accessToken`을 통해 `localStorage`에 저장된 Access Token을 가져옵니다.
        *   요청 헤더에 `config.headers.Authorization = 'Bearer ' + accessToken;` 형태로 Access Token을 추가합니다.
        *   헤더가 추가된 요청은 백엔드로 전송됩니다.

3.  **백엔드: Access Token 유효성 검증**
    *   **위치:** `backend/src/main/java/com/habidue/app/config/jwt/JwtAuthenticationFilter.java`
    *   **주요 클래스/메소드:** `JwtAuthenticationFilter.doFilter()`, `JwtTokenProvider.validateToken()`
    *   **설명:**
        *   백엔드에 요청이 도착하면 `JwtAuthenticationFilter`가 `Authorization` 헤더에서 Access Token을 추출합니다.
        *   `jwtTokenProvider.validateToken(token)`을 호출하여 토큰이 유효한지(만료되지 않았는지, 서명이 올바른지 등) 검증합니다.
        *   **유효한 경우:** `jwtTokenProvider.getAuthentication(token)`을 통해 `Authentication` 객체(여기에 `UserPrincipal` 포함)를 생성하고 `SecurityContextHolder.getContext().setAuthentication()`으로 Spring Security 컨텍스트에 저장합니다. 이제 이 요청은 인증된 상태가 됩니다.
        *   **만료되거나 유효하지 않은 경우:**
            *   `JwtTokenProvider.validateToken()`은 `ExpiredJwtException` (만료) 또는 `false` (유효하지 않음)를 반환합니다.
            *   `JwtAuthenticationFilter`는 이를 감지하여 `JwtAuthenticationEntryPoint.commence()`를 호출합니다.

4.  **백엔드: API 로직 처리**
    *   **위치:** `backend/src/main/java/com/habidue/app/controller/AuthController.java`
    *   **주요 클래스/메소드:** `AuthController.getAuthenticatedUser()`
    *   **설명:**
        *   `SecurityConfig`에 정의된 `requestMatchers("/api/auth/test/**").hasRole("USER")` 조건에 따라, 인증된 사용자가 `ROLE_USER` 권한을 가지고 있으면 컨트롤러로 요청이 전달됩니다.
        *   `@AuthenticationPrincipal UserPrincipal userPrincipal`을 통해 `JwtAuthenticationFilter`가 `SecurityContext`에 저장했던 인증 객체를 주입받습니다.
        *   `ApiResponse.success()`를 통해 사용자 이메일과 ID를 JSON 형태로 응답합니다.

---

## 🔄 3. Access Token 만료 및 Refresh Token 재발급 상세 흐름

**목표:** Access Token이 만료되었을 때, 사용자의 개입 없이 Refresh Token을 사용하여 새로운 Access Token을 자동으로 받아옵니다.

1.  **백엔드: Access Token 만료 감지 & `401 Unauthorized` 응답**
    *   **위치:** `backend/src/main/java/com/habidue/app/config/jwt/JwtTokenProvider.java` -> `validateToken()`
    *   **위치:** `backend/src/main/java/com/habidue/app/config/jwt/JwtAuthenticationFilter.java`
    *   **위치:** `backend/src/main/java/com/habidue/app/config/jwt/JwtAuthenticationEntryPoint.java`
    *   **설명:**
        *   프론트엔드에서 API 요청을 보냈을 때 `JwtAuthenticationFilter`가 `Authorization` 헤더의 Access Token을 `jwtTokenProvider.validateToken()`으로 검증합니다.
        *   Access Token이 만료(`ExpiredJwtException`)되면 `JwtAuthenticationFilter`는 `jwtAuthenticationEntryPoint.commence()`를 호출합니다.
        *   `JwtAuthenticationEntryPoint`는 `ObjectMapper`를 사용하여 `ApiResponse` 형태의 `{"status":401, "message":"토큰이 만료되었습니다."}` JSON 응답을 보냅니다.

2.  **프론트엔드: `401` 응답 감지 & Refresh Token 재발급 요청**
    *   **위치:** `frontend/src/plugins/axios.js`
    *   **주요 클래스/메소드:** `instance.interceptors.response.use(..., async (error) => { ... })`
    *   **설명:**
        *   `axios`의 응답 인터셉터가 백엔드로부터 `error.response.status === 401` (Unauthorized) 응답을 받으면 Access Token이 만료되었다고 판단합니다.
        *   `originalRequest._retry = true` 플래그를 설정하여 무한 루프를 방지합니다.
        *   `useAuthStore().refreshToken`을 통해 `localStorage`에 저장된 Refresh Token을 가져옵니다.
        *   `instance.post('/api/auth/reissue', null, { headers: { 'Authorization-Refresh': refreshToken } })`를 호출하여 백엔드의 재발급 API에 Refresh Token을 보냅니다. (이때 `Authorization` 헤더는 Request 인터셉터에서 명시적으로 제거됩니다.)

3.  **백엔드: Refresh Token 유효성 검증 & 새 Access Token 발급**
    *   **위치:** `backend/src/main/java/com/habidue/app/service/AuthService.java`
    *   **주요 클래스/메소드:** `AuthService.reissueAccessToken()`
    *   **설명:**
        *   `/api/auth/reissue` 엔드포인트는 `SecurityConfig`에서 `permitAll()`로 설정되어 JWT 필터를 통과하지 않습니다.
        *   `AuthService`는 요청 헤더의 `Authorization-Refresh`에서 Refresh Token을 추출합니다.
        *   `jwtTokenProvider.validateToken(refreshToken)`으로 Refresh Token이 유효한지 검증합니다. (만료 여부 포함)
        *   Refresh Token의 `subject` (이메일)을 추출하여 `redisTemplate.opsForValue().get(email)`을 통해 Redis에 저장된 Refresh Token과 일치하는지 확인합니다.
        *   모든 검증이 통과하면 `userRepository.findByEmail(email)`을 통해 `User` 정보를 조회하고, `jwtTokenProvider.createAccessToken(authentication)`을 호출하여 **새로운 Access Token**을 생성합니다.
        *   새로운 Access Token은 응답 헤더 (`Authorization: Bearer [newAccessToken]`)에 담겨 프론트엔드로 반환됩니다.

4.  **프론트엔드: 새 Access Token 저장 및 원래 API 재시도**
    *   **위치:** `frontend/src/plugins/axios.js`
    *   **주요 클래스/메소드:** `instance.interceptors.response.use(..., async (error) => { ... })`
    *   **설명:**
        *   `axios` 인터셉터는 재발급 API로부터 새로운 Access Token을 받습니다.
        *   `useAuthStore().setTokens()`를 호출하여 `localStorage`와 Pinia 스토어의 Access Token을 새 값으로 갱신합니다.
        *   `originalRequest.headers.Authorization = 'Bearer ' + newAccessToken;`를 통해 원래 요청의 헤더를 업데이트합니다.
        *   `return instance(originalRequest);`를 호출하여 **원본 API 요청(`GET /api/auth/test/user`)을 새로운 Access Token으로 재시도**합니다.
        *   이번에는 Access Token이 유효하므로 API 호출이 성공하고 사용자에게 정상적인 응답이 전달됩니다.

---

## ✅ 핵심 개념 요약

*   **JPA `@ManyToOne` & `@JoinColumn`:** 엔티티 간 연관 관계 정의.
*   **`@Transactional(readOnly = true)`:** 읽기 전용 트랜잭션으로 성능 최적화.
*   **`@Builder`:** 객체 생성 편의성.
*   **`@UniqueConstraint`:** DB 레벨에서의 데이터 중복 방지.
*   **`OAuth2 (Open Authorization)`:** 사용자 비밀번호 없이 인증을 위임하는 표준.
*   **`JWT (JSON Web Token)`:** 토큰 기반 인증의 핵심. `헤더.페이로드.서명` 구조.
*   **`Access Token`:** 실제 API 접근에 사용되는 짧은 만료 시간의 토큰.
*   **`Refresh Token`:** Access Token 만료 시 재발급에 사용되는 긴 만료 시간의 토큰. Redis에 저장하여 관리.
*   **`Redis`:** In-memory Key-Value 저장소. Refresh Token 저장 및 캐싱에 활용.
*   **`Spring Security`:** 인증/인가 프레임워크. 필터 체인을 통해 요청을 가로채고 처리.
*   **`AuthenticationEntryPoint`:** 인증 실패 시 클라이언트에게 `401 Unauthorized` 응답을 보내는 역할.
*   **`axios` 인터셉터:** 프론트엔드에서 HTTP 요청/응답을 가로채 토큰 자동 주입 및 만료 토큰 재발급 처리.
*   **`CORS (Cross-Origin Resource Sharing)`:** 다른 도메인 간의 HTTP 요청을 허용하기 위한 브라우저 보안 정책. 백엔드에서 허용 설정 필수.

---

## 💡 이 문서의 활용법
이 문서는 HabiDue 프로젝트의 인증 메커니즘을 이해하고, 향후 기능 확장 및 버그 발생 시 원인을 추적하는 데 매우 유용한 가이드가 될 것입니다. 각 컴포넌트의 역할과 데이터 흐름을 명확히 파악하는 데 도움을 줄 것입니다.
