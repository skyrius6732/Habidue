# 📔 HabiDue Work Summary (2026-03-28)

## 🎯 작업 개요
- **주제:** 카카오 및 네이버 연동 로그인(OAuth2) 구축 및 프론트엔드 통합
- **목적:** 구글 외 국내 주요 소셜 계정 연동을 통해 사용자 접근성 및 편의성 강화
- **수행자:** Gemini CLI (Senior Engineer Persona)

## 🛠️ 기술적 의사결정 (ADR)

### 1. 전략 패턴(Strategy Pattern) 기반의 `OAuth2UserInfo` 추상화
- **의사결정:** 각 소셜 플랫폼(Google, Kakao, Naver)의 서로 다른 응답 JSON 구조를 직접 처리하지 않고, 공통 인터페이스(`OAuth2UserInfo`)를 통해 캡슐화함.
- **이유:** 서비스 로직(`CustomOAuth2UserService`)의 복잡도를 낮추고, 향후 Apple이나 Facebook 등 새로운 플랫폼 추가 시 기존 코드를 수정하지 않고 확장하기 위함 (Open-Closed Principle 준수).

### 2. 고유 식별자(ID) 기반 로그인 및 이메일 통합 로직
- **의사결정:** 네이버나 카카오에서 주는 '변경 가능한 이메일' 대신, 플랫폼이 제공하는 '불변의 ID'를 고유 식별자로 사용하되, 계정 통합을 위해 이메일 매칭 로직을 유지함.
- **이유:** 사용자가 소셜 플랫폼에서 이메일을 변경하더라도 HabiDue 서비스의 로그인이 끊기지 않도록 데이터 정합성을 확보하기 위함.

## 💻 상세 구현 내용

### [Backend]
- **`application.yml` 설정:** 카카오/네이버의 `registration` 및 `provider` 정보를 추가하여 Spring Security OAuth2 Client 인프라 연동.
- **`OAuth2UserInfo` 인터페이스:** `getProvider()`, `getProviderId()`, `getEmail()`, `getName()` 공통 규격 정의.
- **구현체 클래스:**
  - `GoogleUserInfo`: 평면 구조 데이터 처리.
  - `NaverUserInfo`: `response` 키 하위의 중첩 데이터 처리.
  - `KakaoUserInfo`: `id` 및 `kakao_account.profile` 하위 데이터 처리.
- **`CustomOAuth2UserService` 리팩토링:** `registrationId`에 따라 적절한 매퍼를 동적으로 생성하여 처리하도록 고도화.

### [Frontend]
- **`HomeView.vue` 업데이트:** 
  - 네이버 로그인 버튼 신규 추가 및 카카오 로그인 버튼 로직 활성화.
  - 인스타그램 감성의 세련된 UI 스타일(둥근 모서리, 클릭 애니메이션) 및 브랜드 컬러 적용.
  - 백엔드 OAuth2 엔드포인트(`/oauth2/authorization/{provider}`) 연동.

## ⚠️ 향후 필수 조치 사항 (Action Items)
- **개발자 센터 설정:** 
  - 카카오/네이버 개발자 센터에서 `Client ID`와 `Secret`을 발급받아 `application.yml`의 플레이스홀더(`YOUR_...`)를 교체해야 함.
  - 카카오의 경우 `REST API 키`를 사용하며, 필요 시 `Client Secret` 활성화를 권장함.
- **Redirect URI 검증:** 
  - 운영 서버 배포 시 `localhost:8081` 주소를 실제 도메인 주소로 일괄 변경해야 함.

---
*본 문서는 habiDue 프로젝트의 지속 가능한 개발을 위해 작성되었습니다.*
