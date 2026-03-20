# HabiDue 프로젝트 2주차 요약: 핵심 비즈니스 로직 완성 및 JPA 관계 심화

## 🎯 프로젝트 목표
2주차의 목표는 HabiDue 서비스의 핵심 비즈니스 로직인 **공고(Notice), 키워드(Keyword), 관심 공고(UserNotice)**에 대한 **CRUD(Create, Read, Update, Delete) API**를 완성하고, 이 과정에서 발생할 수 있는 데이터베이스 연관 관계 및 성능 이슈에 대한 이해를 높이는 것이었습니다. 또한, API의 일관성을 위해 통일된 오류 처리 방식을 도입했습니다.

## 🛠️ 구현된 도메인 및 기능

### **1. 공고(Notice) 기능**
*   **도메인:** `Notice` 엔티티 (제목, 내용, 마감일, 링크, 출처 등 포함)
*   **주요 기능:**
    *   **생성 (`POST /api/notices`):** 새로운 공고를 등록합니다. (`link` 중복 시 예외 처리)
    *   **조회 (`GET /api/notices`):** 모든 공고를 페이징하여 조회하며, 키워드(`keyword`)를 통한 제목/내용 검색 기능도 포함합니다.
    *   **단일 조회 (`GET /api/notices/{id}`):** 특정 ID의 공고를 조회합니다.
    *   **수정 (`PUT /api/notices/{id}`):** 특정 공고의 내용을 수정합니다. (`link` 중복 시 예외 처리)
    *   **삭제 (`DELETE /api/notices/{id}`):** 특정 공고를 삭제합니다.
*   **특징:** 공고는 주로 크롤링을 통해 수집되므로 `link` 필드에 `unique=true` 제약 조건을 두어 중복을 방지합니다.

### **2. 키워드(Keyword) 기능**
*   **도메인:** `Keyword` 엔티티 (키워드 문자열, 소유 사용자 `User` 연관 관계)
*   **주요 기능:**
    *   **등록 (`POST /api/keywords`):** 사용자가 자신의 관심 키워드를 등록합니다. (`사용자별 키워드 중복` 시 예외 처리)
    *   **조회 (`GET /api/keywords`):** 현재 로그인한 사용자가 등록한 모든 키워드 목록을 조회합니다.
    *   **삭제 (`DELETE /api/keywords/{id}`):** 사용자가 자신의 특정 키워드를 삭제합니다.
*   **특징:** `user_id`와 `word`의 복합 `UniqueConstraint`를 통해 사용자별로 동일한 키워드를 중복 등록할 수 없도록 합니다.

### **3. 관심 공고(UserNotice) 기능**
*   **도메인:** `UserNotice` 엔티티 (관심 공고를 등록한 `User`, 대상 `Notice` 연관 관계, 메모 필드)
*   **주요 기능:**
    *   **등록 (`POST /api/user-notices`):** 사용자가 특정 공고를 자신의 관심 공고로 스크랩합니다. (`사용자별 공고 중복` 시 예외 처리)
    *   **조회 (`GET /api/user-notices`):** 현재 로그인한 사용자가 스크랩한 모든 관심 공고 목록을 페이징하여 조회합니다.
    *   **메모 수정 (`PUT /api/user-notices/{id}/memo`):** 스크랩한 공고에 대한 메모를 수정합니다.
    *   **삭제 (`DELETE /api/user-notices/{id}`):** 스크랩한 관심 공고를 삭제합니다.
*   **특징:** `user_id`와 `notice_id`의 복합 `UniqueConstraint`를 통해 사용자가 동일한 공고를 여러 번 스크랩할 수 없도록 합니다.

## 💡 JPA 연관 관계 및 성능 최적화 개념 (알기 쉽게 설명)

### **1. 연관 관계 매핑 (예: `@ManyToOne`)**
*   **`Keyword` 엔티티와 `User` 엔티티의 관계:** 하나의 `User`는 여러 개의 `Keyword`를 가질 수 있고, 하나의 `Keyword`는 오직 하나의 `User`에게만 속합니다. 이를 **다대일(ManyToOne)** 관계라고 합니다.
*   **`@ManyToOne`:** `Keyword` 엔티티의 `user` 필드 위에 붙어 있습니다. `(fetch = FetchType.LAZY)`는 지연 로딩을 의미합니다.
*   **`@JoinColumn(name = "user_id", nullable = false)`:** `Keyword` 테이블에 `user_id`라는 외래 키(FK) 컬럼이 생기고, 이 컬럼은 `User` 테이블의 PK를 참조하며 `NULL`을 허용하지 않습니다.

### **2. 지연 로딩(FetchType.LAZY)과 N+1 문제**
*   **지연 로딩(Lazy Loading)의 필요성:**
    *   `Keyword`를 조회할 때, 당장 `User` 정보가 필요하지 않다면 `User` 정보까지 DB에서 함께 가져올 필요가 없습니다. `fetch = FetchType.LAZY`는 "지금 당장 `User` 객체는 가져오지 말고, 나중에 `keyword.getUser()`처럼 실제로 사용될 때 가져와라"고 지시하는 것입니다.
    *   **장점:** 불필요한 DB 조회를 줄여 성능을 향상시킵니다.
*   **N+1 문제란? (지연 로딩의 함정)**
    *   만약 `List<Keyword>`를 조회한다고 가정해 봅시다.
    *   1. `Keyword` 목록을 가져오는 쿼리 1번 실행 (N개의 Keyword 엔티티).
    *   2. 그리고 만약 각 `Keyword`에 연결된 `User`의 정보를 사용 (`keyword.getUser().getEmail()`)한다면, 각각의 `Keyword`마다 `User` 정보를 가져오기 위해 쿼리가 N번 추가로 실행됩니다.
    *   결과적으로 총 `1 + N`번의 쿼리가 발생하여 성능 저하를 일으키는 문제를 N+1 문제라고 합니다.
*   **해결 전략 (코드에는 직접 적용하지 않았지만 개념 중요):**
    *   **Fetch Join:** JPQL(Java Persistence Query Language)에서 `JOIN FETCH`를 사용하여 N개의 `Keyword`를 가져올 때, 연관된 `User`도 한 번의 쿼리로 함께 가져오도록 합니다. `Repository`에 `@Query("SELECT k FROM Keyword k JOIN FETCH k.user WHERE k.user.id = :userId")` 와 같은 형태로 정의할 수 있습니다.
    *   **`@EntityGraph`:** `@ManyToOne` 관계에서 필요한 필드를 미리 로드하도록 설정하여 N+1 문제를 자동으로 해결해 줍니다.

### **3. `@Transactional(readOnly = true)`의 이점**
*   **`NoticeService`, `KeywordService`, `UserNoticeService`**에 공통적으로 적용했습니다.
*   **의미:** 이 서비스 메소드 안에서는 데이터 변경(INSERT, UPDATE, DELETE)이 발생하지 않고 오직 조회(`SELECT`)만 할 것이라고 JPA에게 알려줍니다.
*   **장점:**
    *   **성능 향상:** JPA가 변경 감지(Dirty Checking)를 위한 스냅샷 저장 등의 추가 작업을 수행하지 않아 효율적입니다.
    *   **데이터 무결성:** 실수로 읽기 전용 트랜잭션 내에서 데이터를 변경하려 하면 예외를 발생시켜 데이터 일관성을 지켜줍니다.

### **4. `@Builder` 패턴**
*   `Notice`, `Keyword`, `UserNotice` 엔티티와 DTO에 적용했습니다.
*   **장점:** 객체를 생성할 때 어떤 필드에 어떤 값을 넣는지 직관적으로 알 수 있어 코드 가독성이 좋아지고, 생성자의 인자 순서에 얽매이지 않아 유연합니다.

### **5. `UniqueConstraint` (DB 레벨 제약 조건)**
*   `Keyword` 엔티티: `@UniqueConstraint(columnNames = {"user_id", "word"})`
*   `UserNotice` 엔티티: `@UniqueConstraint(columnNames = {"user_id", "notice_id"})`
*   **장점:** 애플리케이션 레벨에서 중복 검사 로직(`existsBy...`)을 수행하기 전에, 데이터베이스 자체에서 중복을 허용하지 않도록 강제하여 데이터의 무결성을 보장합니다.

## ✅ 2주차 최종 달성 목표
1.  **Notice, Keyword, UserNotice** 3가지 핵심 도메인의 CRUD API 완벽 구현.
2.  사용자별 키워드 및 관심 공고 관리 기능 구현.
3.  JPA 연관 관계 및 지연 로딩 개념을 실제 코드에 적용.
4.  통일된 `ApiResponse` 및 `GlobalExceptionHandler`를 통한 일관된 오류 처리.

이것으로 2주차의 모든 개발 목표가 달성되었습니다! 다음은 **3주차: 프론트엔드 연동 및 크롤러 개발**입니다.
