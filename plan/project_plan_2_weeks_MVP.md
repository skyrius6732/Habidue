# HabiDue: 2주 완성 초고속 MVP 계획표

**프로젝트명:** HabiDue (Habit + Due)
**목표:** 2주 안에 핵심 기능을 구현하고 AWS에 배포하여 동작하는 URL을 확보한다.
**핵심 전략:** 선택과 집중. **캘린더 뷰, 캐싱 등은 과감히 제외**하고, `인증 -> 크롤링 -> 공고 리스트 API -> 최소한의 UI` 흐름을 최우선으로 완성한다.

---

### **📅 1주차: 백엔드 API 완성 (Backend API Completion)**
> **주간 목표: 인증, 공고/키워드 API, 첫 크롤러까지 백엔드의 모든 핵심 기능을 완성한다.**

- **Day 1-2 (월,화): 인증/인가 집중 개발**
  - Spring Boot 프로젝트 생성 및 DB 연결
  - User 엔티티, Spring Security, JWT 기반의 회원가입/로그인 API를 최우선으로 구현
  - **결과물:** Postman으로 로그인 시 토큰이 발급되는 것을 확인.

- **Day 3-4 (수,목): 핵심 비즈니스 로직 API**
  - Notice, Keyword, UserNotice 엔티티 설계
  - 공고 목록 조회 API (`GET /api/notices`)
  - 키워드 생성/조회 API (`POST, GET /api/keywords`)
  - **결과물:** Postman으로 모든 핵심 API의 요청/응답 확인.

- **Day 5-7 (금-일): 첫 크롤러 개발 및 API 통합 테스트**
  - 첫 번째 타겟 사이트(예: LH) 크롤러 로직 구현
  - `@Scheduled`를 이용해 주기적으로 DB에 공고 데이터 적재
  - 크롤링된 데이터가 공고 목록 API를 통해 정상적으로 조회되는지 통합 테스트
  - **결과물:** 스케줄러가 동작하여 DB에 데이터가 쌓이고, API로 조회 가능.

---

### **📅 2주차: 최소 기능 프론트엔드 및 배포 (Minimal Frontend & Deployment)**
> **주간 목표: 백엔드 API와 연동되는 최소한의 화면을 만들고, AWS에 배포하여 외부에서 접속 가능하게 한다.**

- **Day 1-2 (월,화): 최소 기능 프론트엔드 (Minimal Viable Frontend)**
  - Vue.js 프로젝트 생성 및 Axios 연동
  - **(필수)** 로그인 페이지, 공고 리스트 페이지만 구현. (캘린더 뷰는 MVP 이후 구현)
  - 로그인 후, 공고 API를 호출하여 목록을 화면에 뿌려주는 기능에만 집중.
  - **결과물:** 로컬에서 로그인하고, 크롤링된 공고 목록을 눈으로 확인 가능.

- **Day 3-4 (수,목): Docker化 및 AWS 인프라 준비**
  - `Dockerfile`, `docker-compose.yml` 작성 (app, mysql)
  - AWS EC2 인스턴스(t3.small) 생성 및 Docker 설치
  - **결과물:** EC2 서버에 접속하여 Docker가 실행되는 것을 확인.

- **Day 5-7 (금-일): AWS 배포 및 최종 정리**
  - EC2 서버에 프로젝트 클론 및 `docker-compose up`으로 서비스 실행
  - IP 주소와 포트 번호로 외부에서 서비스 접속 테스트
  - GitHub `README.md` 파일에 프로젝트 실행 방법 및 API 명세 간단히 정리
  - **결과물:** 외부에서 접속 가능한 URL(IP:Port) 확보.

---

### **⚠️ MVP에서 제외된 항목 (Post-MVP Scope)**
- 캘린더 뷰 UI
- Redis 캐싱을 통한 성능 최적화
- Nginx, HTTPS 적용
- 상세한 예외 처리 및 테스트 코드
