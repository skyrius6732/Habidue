# HabiDue 프로젝트 1주차 요약: 구글 OAuth2 + JWT 인증 서버 구축

## 🎯 프로젝트 목표
HabiDue는 사용자의 관심 키워드를 기반으로 마감 일정을 자동으로 관리해주는 서비스입니다. 1주차의 목표는 이 서비스의 핵심인 **인증(Authentication) 및 인가(Authorization) 서버**를 Spring Boot 기반으로 구축하는 것이었습니다. 특히, 사용자의 편의성과 사업적 가치를 고려하여 **구글 소셜 로그인(OAuth2) 중심의 JWT 인증 방식**을 채택했습니다.

## 🛠️ 주요 기술 스택
*   **백엔드 프레임워크:** Spring Boot 3.x
*   **데이터베이스:** MySQL 8.0 (Docker)
*   **데이터 캐시/세션:** Redis (Docker)
*   **인증/인가:** Spring Security, OAuth2 Client, JWT (jjwt 라이브러리)
*   **영속성:** Spring Data JPA (Hibernate)

## 📅 1주차 진행 과정 및 핵심 개념

### **1. 프로젝트 초기 환경 설정 및 DB 연결 안정화 (Day 1-2)**
*   **내용:** Spring Boot 프로젝트를 생성하고, MySQL 및 Redis 연결을 위한 `application.yml` 설정을 진행했습니다. `User` 엔티티와 기본적인 `UserRepository`를 설계했습니다.
*   **핵심 개념:**
    *   **`application.yml`:** Spring Boot 애플리케이션의 설정 파일. 데이터베이스, 포트, JWT 시크릿 키 등 모든 중요한 설정이 여기에 기록됩니다.
    *   **Spring Data JPA:** Java 객체(엔티티)를 데이터베이스 테이블과 매핑하여, SQL 쿼리 없이도 데이터를 쉽게 조작할 수 있게 해주는 기술입니다.
*   **주요 이슈 & 해결:**
    *   **MySQL 연결 오류:** Windows IntelliJ에서 WSL2 Docker MySQL 컨테이너로 접속 시 `GSS-API authentication exception` 오류 발생.
    *   **해결:**
        *   DB 유저 (`root`, `habidue_user`)의 인증 플러그인을 `mysql_native_password`로 명시적 변경.
        *   WSL2 Docker의 MySQL 컨테이너를 재생성하며 `default-authentication-plugin`을 `mysql_native_password`로 강제 지정.
        *   `application.yml`의 DB 접속 주소를 `127.0.0.1` 대신 WSL2의 실제 IP (`172.x.x.x`)로 변경하여 Windows의 다른 DB 서비스와의 충돌을 회피.
        *   테스트 환경(`src/test/resources/application.yml`)에 가짜 OAuth2 설정 및 H2 DB 설정을 추가하여 빌드 안정성 확보.

### **2. 구글 OAuth2 소셜 로그인 연동 (Day 3-4)**
*   **내용:** 사용자가 구글 계정으로 HabiDue 서비스에 로그인할 수 있도록 Spring Security OAuth2 Client를 설정하고 구현했습니다.
*   **구글 클라우드 콘솔 설정:**
    *   **Google Cloud Console**에서 프로젝트 생성.
    *   **API 및 서비스 > OAuth 동의 화면** 설정 (애플리케이션 이름: HabiDue, 사용자 지원 이메일 등).
    *   **API 및 서비스 > 사용자 인증 정보 > OAuth 클라이언트 ID** 생성 (`웹 애플리케이션` 타입).
    *   **승인된 리다이렉트 URI:** `http://localhost:8081/login/oauth2/code/google` 등록.
    *   발급된 **`client-id`**와 **`client-secret`**을 `application.yml`에 설정.
*   **핵심 개념:**
    *   **OAuth2 (Open Authorization 2.0):** 사용자 ID/PW를 직접 받지 않고, 구글 같은 서비스에 "너 이 사람 알아? 맞으면 나에게 허가증 줘"라고 요청하여 인증하는 개방형 표준 프로토콜.
    *   **`client-id` & `client-secret`:** 구글 클라우드 콘솔에서 발급받는 우리 서비스의 '신분증' 같은 정보.
    *   **`redirect-uri`:** 구글 로그인이 성공했을 때, 구글이 사용자 브라우저를 다시 돌려보낼 우리 서비스의 주소 (`/login/oauth2/code/google`).
    *   **`CustomOAuth2UserService`:** 구글 로그인 성공 후, 구글에서 받은 사용자 정보(이메일, 이름 등)를 우리 DB(`User` 엔티티)에 저장하거나 업데이트하는 비즈니스 로직 담당. 이 과정에서 `UserPrincipal`을 생성하여 스프링 시큐리티의 인증 객체로 활용합니다.
    *   **`UserPrincipal`:** 우리 서비스의 `User` 객체와 스프링 시큐리티의 `UserDetails`, `OAuth2User` 인터페이스를 연결해주는 어댑터 역할. JWT의 Subject(주체)가 됩니다.

### **3. JWT(JSON Web Token) 발급 및 인증 (Day 3-4, Day 5-7)**
*   **내용:** 구글 로그인 성공 후, 우리 서비스 전용 '통행증'인 JWT(Access Token, Refresh Token)를 발급하고, 이후 API 요청 시 이 토큰의 유효성을 검증하는 시스템을 구축했습니다.
*   **핵심 개념:**
    *   **JWT 구조:** `헤더.페이로드.서명` (세 부분으로 구성된 문자열).
        *   **헤더:** 토큰 타입(JWT)과 서명 알고리즘(HS256) 명시. (Base64 인코딩)
        *   **페이로드:** 실제 정보(사용자 ID, 이메일, 권한, 만료 시간 등)를 담고 있음. (Base64 인코딩)
        *   **서명:** 헤더와 페이로드를 서버만 아는 **비밀 키(`jwt.secret`)**로 해싱하여 생성. 토큰의 위변조 여부를 검증하는 데 사용됩니다.
    *   **Access Token:** 만료 시간이 짧은(1시간) 토큰. 실제 API 요청 시 `Authorization: Bearer [Access Token]` 형태로 사용됩니다.
    *   **Refresh Token:** 만료 시간이 긴(2주) 토큰. Access Token이 만료되었을 때 새로운 Access Token을 재발급받는 데 사용됩니다. 보안상 Redis 같은 안전한 저장소에 보관합니다.
    *   **`JwtTokenProvider`:** JWT의 생성, 파싱, 유효성 검증을 담당하는 유틸리티 클래스.
    *   **`OAuth2SuccessHandler`:** 구글 로그인 성공 후, `JwtTokenProvider`를 호출하여 토큰을 발급하고, Refresh Token을 **Redis**에 저장하며, 발급된 토큰들을 프론트엔드로 리다이렉트합니다.
    *   **`JwtAuthenticationFilter`:** 모든 HTTP 요청을 가로채 `Authorization` 헤더에서 Access Token을 추출, `JwtTokenProvider`로 유효성을 검증한 후, 인증된 사용자 정보를 스프링 시큐리티 컨텍스트에 저장합니다.
    *   **`AuthService` (재발급 API):** 프론트엔드로부터 Refresh Token을 받아 유효성 검증(Redis 저장 여부 포함) 후, 새로운 Access Token을 발급하는 로직 담당.
    *   **`application.yml` (JWT 설정):** `jwt.secret`, `access-token-validity-in-seconds`, `refresh-token-validity-in-seconds` 등 JWT 관련 설정 값들을 관리합니다.

### **4. Redis 활용 (Refresh Token 저장)**
*   **내용:** 발급된 Refresh Token은 Redis에 저장하여 관리합니다.
*   **핵심 개념:**
    *   **Redis:** 인메모리(In-Memory) 데이터 저장소로, 빠른 읽기/쓰기 성능을 자랑합니다. 데이터 유실에 민감하지 않은 휘발성 데이터(예: Refresh Token, 캐시)를 저장하는 데 적합합니다.
    *   **TTL (Time To Live):** Redis에 저장된 데이터의 생존 시간을 설정하여, Refresh Token 만료 시 Redis에서도 자동으로 삭제되도록 합니다.
    *   **`RedisTemplate`:** Spring Data Redis에서 Redis와 상호작용하기 위한 주요 인터페이스.

### **5. SecurityConfig 최종 설정**
*   **내용:** 스프링 시큐리티가 JWT를 사용하여 인증 및 인가를 처리하도록 최종 설정했습니다.
*   **핵심 설정:**
    *   `sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`: JWT 사용 시 서버 세션을 사용하지 않는 '무상태' 방식으로 설정.
    *   `.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)`: JWT 필터를 가장 먼저 실행시켜 토큰 유효성을 검사합니다.
    *   `.requestMatchers("/api/test/**").hasRole("USER")`: `/api/test/**` 경로에는 `ROLE_USER` 권한을 가진 사용자만 접근 가능하도록 설정.
    *   `@EnableMethodSecurity`: 컨트롤러 메소드 레벨에서 `@Secured("ROLE_USER")`와 같은 어노테이션으로 세부 권한 설정 가능하게 함.

## ✅ 1주차 최종 검증 완료
1.  구글 로그인 성공 후 Access Token과 Refresh Token이 발행되고, Redis에 Refresh Token이 정상 저장됨.
2.  발급된 Access Token으로 `/api/test/user`에 접근 시 `Authenticated user email: ...` 응답 확인.
3.  Refresh Token으로 새로운 Access Token을 재발급받는 `POST /api/auth/reissue` API까지 구축 완료.

이제 1주차의 모든 목표를 완벽하게 달성했습니다! 다음은 2주차 목표인 **핵심 비즈니스 로직(공고, 키워드, 관심 공고 CRUD)** 구현입니다.
