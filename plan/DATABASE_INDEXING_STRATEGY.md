# habiDue 데이터베이스 인덱싱 최적화 전략

본 문서는 habiDue 커뮤니티의 데이터가 수십만 건 이상으로 늘어날 경우를 대비하여 적용한 데이터베이스 인덱싱(Indexing) 작업 내용을 정리한 것입니다.

## 1. 인덱싱(Indexing)이란?
데이터베이스의 인덱스는 두꺼운 책의 **'찾아보기(색인)'**와 같습니다. 
데이터가 방대해졌을 때 처음부터 끝까지 찾는 'Full Scan' 방식을 피하고, 미리 정렬된 색인 명단을 통해 필요한 데이터의 위치로 즉시 점프하여 검색 속도를 극대화하는 기술입니다.

---

## 2. 적용 대상 및 내역

### A. 게시글 테이블 (posts)
커뮤니티의 핵심 데이터를 담고 있는 테이블로, 상태 필터링과 최신순 정렬이 빈번하게 일어납니다.

- **적용 인덱스:**
  1. `idx_post_status_created`: 게시글 상태(`status`)와 생성일(`createdAt`) 복합 인덱스. (상태별 최신글 조회 최적화)
  2. `idx_post_author`: 작성자(`user_id`) 기준 검색 최적화.
  3. `idx_post_notice`: 특정 공고(`notice_id`)에 달린 소통방 게시글 조회 최적화.

- **자바 엔티티 설정 (`Post.java`):**
```java
@Table(name = "posts", indexes = {
    @Index(name = "idx_post_status_created", columnList = "status, createdAt"),
    @Index(name = "idx_post_author", columnList = "user_id"),
    @Index(name = "idx_post_notice", columnList = "notice_id")
})
```

---

### B. 댓글 테이블 (comments)
게시글 상세 페이지 진입 시 해당 게시글의 모든 댓글을 불러오는 쿼리가 가장 중요합니다.

- **적용 인덱스:**
  1. `idx_comment_status`: 댓글 상태(`status`) 필터링 최적화.
  2. `idx_comment_post`: 게시글(`post_id`)별 댓글 묶음 조회 최적화.

- **자바 엔티티 설정 (`Comment.java`):**
```java
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_status", columnList = "status"),
    @Index(name = "idx_comment_post", columnList = "post_id")
})
```

---

## 3. 트러블슈팅 (Troubleshooting)

### 이슈: `author_id` 컬럼 부재 오류
- **현상:** `create index idx_post_author on posts (author_id)` 실행 시 오류 발생.
- **원인:** 자바 코드상의 필드명은 `author`였으나, 실제 DB 매핑 컬럼명은 `@JoinColumn(name = "user_id")`에 의해 **`user_id`**로 생성되어 있었음.
- **해결:** 인덱스 설정의 `columnList`를 실제 DB 컬럼명인 `user_id`로 수정하여 해결.

---

## 4. 기대 효과
1. **검색 성능 보장:** 수십만 건의 데이터가 쌓여도 `ACTIVE` 상태의 게시글이나 특정 유저의 글을 0.01초 내외로 검색 가능.
2. **운영 안정성:** 관리자 페이지에서 `BLINDED` 또는 `DELETED` 상태의 글을 필터링할 때 DB 부하를 최소화.
3. **확장성:** 향후 실시간 인기글 집계 및 통계 쿼리 실행 시 기본 인덱스를 활용하여 성능 병목 현상 방지.

---

## 5. 직접 실행용 SQL (운영 DB 적용 시)
```sql
-- posts 테이블
CREATE INDEX idx_post_status_created ON posts (status, created_at);
CREATE INDEX idx_post_author ON posts (user_id);
CREATE INDEX idx_post_notice ON posts (notice_id);

-- comments 테이블
CREATE INDEX idx_comment_status ON comments (status);
CREATE INDEX idx_comment_post ON comments (post_id);
```
