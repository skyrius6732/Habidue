# 📋 ADR: 게시판 시스템 통합 확장 및 단지별 커뮤니케이션 구축

## 1. 개요
habiDue 서비스의 커뮤니티 활성화를 위해 통합광장, 당첨후기, 파트너스 게시판을 일괄 구축하고, 단지별 소통 니즈를 태그 기반 시스템으로 통합광장 내에 수용함.

## 2. 핵심 설계 결정 (Architectural Decisions)

### 2.1. 단일 엔티티 기반 다중 게시판 구조
- **결정:** 모든 게시판은 `Post` 엔티티와 `PostType` 열거형(Enum)으로 관리함.
- **이유:** 테이블 파편화를 방지하고, 검색(QueryDSL) 및 관리 로직을 일원화하여 유지보수 효율성을 극대화함.

### 2.2. 태그 기반 단지별 커뮤니케이션 (Complex-based Tagging)
- **결정:** 별도의 단지별 게시판을 생성하지 않고, `TagType.COMPLEX` 태그를 도입하여 통합광장 내에서 필터링함.
- **이유:** 수만 개의 단지를 각각의 게시판으로 분리할 경우 발생하는 유저 분산 및 관리 오버헤드를 해결하고, 통합광장의 데이터 밀도를 유지함.

### 2.3. 하이브리드 필터링 시스템
- **통합광장(GENERAL):** `category`(FREE, INFO 등)와 `regionTag`(지역), `PostTag`(단지명) 조합 검색.
- **당첨후기(REVIEW):** `category`(INTERVIEW, TIPS 등) 기반 검색.
- **파트너스(PARTNER):** `category`(MOVING, CLEANING 등) 기반 검색 + 업체 중심 카드 UI.

## 3. 세부 구현 계획

### 3.1. 백엔드 (Spring Boot & QueryDSL)
- **TagType 확장:** `COMPLEX` 타입 추가.
- **PostRepositoryImpl:** 
    - `booleanExpression`을 활용한 동적 필터링 고도화.
    - `PostTag`와 조인하여 특정 태그(단지명) 검색 로직 추가.
- **PostService:** 
    - 게시판 타입별 작성 권한 검증 (예: PARTNER 게시판은 특정 권한 필요).

### 3.2. 프론트엔드 (Vue.js)
- **BoardView.vue:** 
    - 전역 게시판 엔진으로 진화.
    - `activeMenu`, `activeSubCategory` 상태를 API 호출 파라미터로 자동 변환.
- **PostWriteView.vue:** 
    - 게시판 타입에 따른 가이드 메시지 및 카테고리/태그 입력 UI 동적 전환.
- **CommunitySidebar.vue:** 
    - 세부 카테고리 클릭 시 즉각적인 목록 갱신 연동.

## 4. 기대 효과
- 사용자는 복잡한 메뉴 이동 없이 태그와 필터만으로 관심 단지와 지역 정보를 빠르게 습득 가능함.
- 운영자는 `PostStatus.BLINDED` 등 공통 관리 기능을 통해 모든 게시판을 일관되게 통제할 수 있음.
