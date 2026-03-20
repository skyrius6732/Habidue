# 🔍 habiDue 통합 스마트 검색 시스템 설계 (v1.0)

## 1. 목적 및 배경
`habiDue`는 지역별 소통방, 통합 광장, 당첨 후기 등 게시판이 세분화되어 있어, 사용자가 원하는 정보를 찾기 위해 각 게시판을 일일이 방문해야 하는 불편함이 있음. 이를 해결하기 위해 서비스 전체를 아우르는 **'통합 문맥 인지 검색'** 시스템을 구축하여 탐색 경험을 혁신함.

## 2. 주요 기능 및 범위

### A. 글로벌 통합 검색 (Global Search)
- **대상:** 모든 활성 게시글(`Post` 엔티티, `status = 'ACTIVE'`)
- **검색 필드:** 제목(`title`), 본문(`content`), 태그(`tags.name`), 작성자(`author.nickname`)
- **결과 분류:**
  - 공고 소통방 (Notice Communication)
  - 통합 광장 (General Community)
  - 당첨 후기 (Winning Reviews)
  - 정보 공유 (Info/Tips)

### B. 문맥 인지 검색 (Contextual Search)
- **현재 위치 유지:** 특정 게시판 진입 시 검색창은 기본적으로 해당 게시판 내 결과를 필터링하여 노출.
- **확장 버튼:** 결과가 적거나 다른 게시판 결과가 궁금할 때 "전체 게시판 결과 보기"로 즉시 전환 제공.

### C. 지능형 정렬 및 가중치 (Ranking)
- **우선순위 로직:** `(제목 일치 * 10) + (본문 일치 * 5) + (태그 일치 * 7) + (조회수 * 0.1) + (좋아요 * 0.5)`
- **최신성:** 동일 조건일 경우 최신글 우선 노출.

## 3. 기술 설계 (Technical Design)

### A. Backend (Spring Boot + QueryDSL)
- **API Endpoint:** `GET /api/v1/search?keyword={keyword}&type={type}&page={page}`
- **Query Strategy:** 
  - QueryDSL의 동적 쿼리를 사용하여 `keyword`가 포함된 게시글을 검색.
  - `PostType`별로 그룹화하여 응답하거나, 전체 리스트를 가중치 순으로 반환.
  - N+1 방지를 위해 `fetchJoin` 및 `BatchSize` 적극 활용.

### B. Frontend (Vue.js)
- **Global Header:** 공통 헤더에 검색창(Search Bar) 고정.
- **Search Result Page:** 
  - 키워드 하이라이팅 적용 (검색어와 일치하는 부분 강조).
  - 섹션별(게시판별) 검색 결과 레이아웃 구축.
  - 무한 스크롤(Infinite Scroll) 지원.

### C. UX Detail
- **Snippet 추출:** 본문 검색 시 검색어가 포함된 앞뒤 100자 내외를 추출하여 결과에 노출.
- **No Result State:** 검색 결과가 없을 때 "이런 키워드는 어때요?"와 같은 추천 검색어 제공.

## 4. 데이터베이스 인덱싱 전략
- `posts` 테이블의 `title` 컬럼에 대해 인덱스 추가 검토.
- (향후 확장) 데이터량이 많아질 경우 MySQL `FULLTEXT INDEX` 또는 Elasticsearch 도입 고려.

## 5. 단계별 실행 계획
1. **[Step 1]** 통합 검색 전용 API 개발 (QueryDSL 가중치 쿼리 포함)
2. **[Step 2]** 검색 결과 DTO 정의 및 Snippet 추출 로직 구현
3. **[Step 3]** 프론트엔드 통합 검색 결과 페이지 UI 구축
4. **[Step 4]** 키워드 하이라이팅 및 최근 검색어 로컬 저장소 연동
