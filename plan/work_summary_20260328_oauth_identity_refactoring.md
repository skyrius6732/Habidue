# [ADR] 2026-03-28 OAuth2 계정 식별 체계 고도화 및 플랫폼별 계정 분리

## 1. 배경 및 문제점 (Context)
- **이메일 기반 병합 결함:** 기존 로직은 `email`을 유일 식별자로 사용하여, 서로 다른 소셜 서비스(카카오, 네이버)를 쓰더라도 이메일이 같으면 강제로 하나의 계정으로 병합됨.
- **데이터 정합성 및 보안 위험:** 사용자의 명시적 동의 없는 계정 병합은 데이터 소유권 분쟁 및 소셜 계정 탈취 시 피해 확산의 원인이 됨.
- **DB 제약 조건 충돌:** `users.email` 컬럼의 `UNIQUE` 제약 조건으로 인해 플랫폼별 독립 계정 생성이 원천적으로 불가능한 상태.

## 2. 해결 방안 (Decision)
### A. 식별 기준의 전환 (Identity Shift)
- **Primary Identifier:** `email` → `(provider, providerId)` 복합 식별자 사용.
- **Security Identifier:** Spring Security 및 JWT 토큰의 주 식별자를 `email`에서 시스템 고유 아이디인 `username`으로 변경.

### B. 데이터베이스 스키마 최적화
- `users.email`의 `UNIQUE` 제약 조건을 제거하여 이메일 중복 허용.
- `(provider, providerId)` 복합 유니크 제약 조건을 추가하여 소셜 계정의 중복 생성 방지.

### C. 전역적 조회 로직 쇄신 (Refactoring)
- 인증 객체(`UserPrincipal`) 및 토큰(`JwtTokenProvider`)의 `subject`를 `username`으로 통일.
- 컨트롤러 및 서비스의 사용자 조회 쿼리를 `findByEmail`에서 `findByUsername`으로 전면 교체.

## 3. 상세 수정 내역 (Technical Details)
### 1) 엔티티 수정 (`User.java`)
- `email` 컬럼의 `unique = true` 속성 제거.
- `@Table(uniqueConstraints = ...)`를 통해 `(provider, providerId)` 복합 유니크 인덱스 추가.

### 2) 인증 아키텍처 수정 (`UserPrincipal`, `JwtTokenProvider`)
- `UserPrincipal`: `getUsername()` 및 `getName()`이 이제 고유한 `username`을 반환함.
- `JwtTokenProvider`: JWT 토큰의 `subject`를 `username`으로 설정하여 발행 및 검증 수행.

### 3) 소셜 로그인 서비스 수정 (`CustomOAuth2UserService`)
- 이메일 기반 자동 병합 로직(`findByEmail`) 완전 제거.
- 신규 가입 시 소셜 서비스의 이름이 기존 닉네임과 충돌할 경우 `providerId` 기반의 접미사(Suffix)를 붙여 자동 회피 처리.

### 4) 인프라 및 서비스 연동
- **Redis 키 변경:** Refresh Token의 저장 키를 `email`에서 `username`으로 변경하여 토큰 재발급 정합성 확보.
- **전역 교체:** `AuthService`, `UserService`, `UserController`, `NoticeController` 등 20여 곳의 조회 로직 수정 완료.

## 4. 보안 및 정합성 검토 (Security Audit)
- **ID 중복 방지:** `username`은 `이름_providerId(5자)` 조합으로 생성되며, DB 레벨의 `UNIQUE` 제약 조건이 이중으로 방어함.
- **닉네임 중복 방지:** 닉네임 변경 시 `existsByNickname` 체크를 수행하므로 시스템 내 동일 닉네임 사용자는 존재할 수 없음.
- **보안 격리:** 이메일이 같더라도 로그인 방식(Provider)이 다르면 서로의 데이터에 접근할 수 없도록 완벽하게 격리됨.

## 5. 향후 주의사항
- **JWT 무효화:** 식별 체계 변경으로 인해 기존에 발급된(이메일 기반) JWT 토큰은 더 이상 유효하지 않음. 사용자는 재로그인이 필요함.
- **DB 인덱스:** 수동으로 제거한 `users.email` 유니크 인덱스(`UK6dotkott2kjsp8vw4d0m25fb7`)가 운영 환경에 반영되었는지 반드시 확인 필요.

---
**작성일:** 2026-03-28  
**상태:** 승인 및 구현 완료  
**영향 범위:** 인증/인가, 회원 관리, 소셜 로그인 전반
