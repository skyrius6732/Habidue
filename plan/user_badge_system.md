# 🎖️ 활동 기반 배지 및 통계 시스템 설계 (User Badge System)

## 1. 개요
유저의 커뮤니티 활동(글 작성, 댓글, 좋아요, 관심 공고 설정 등)을 실시간으로 추적하여 통계화하고, `board_planner.md`에서 정의한 특정 조건을 달성할 시 배지를 부여함. 이를 통해 커뮤니티 내 신뢰도를 구축하고 유저 참여를 독려함.

## 2. 데이터베이스 설계 (Entity)

### A. UserActivityStats (유저 활동 통계)
- **목적:** 유저별 활동 수치를 실시간으로 보관하여 성능 최적화 (매번 Count 쿼리 방지).
- **필드:**
  - `user_id` (PK, OneToOne with User)
  - `total_post_count`: 총 게시글 수
  - `total_comment_count`: 총 댓글 수
  - `total_like_received_count`: 게시글/댓글로 받은 총 좋아요 수
  - `total_notice_interest_count`: 관심 공고(하트) 설정 수
  - `consecutive_attendance_days`: 연속 출석 일수
  - `last_attendance_date`: 마지막 출석일
  - `review_post_count`: 후기 게시판 작성 글 수
  - `region_activity_json`: 지역별 활동 점수 (JSON 형식 보관 고려)

### B. Badge (배지 정의)
- **필드:** `id`, `code` (예: KNOWLEDGE_1), `name`, `description`, `icon_url`, `type` (정보형, 성실형 등), `level`.

### C. UserBadge (유저 획득 배지)
- **필드:** `user_id`, `badge_id`, `created_at`.

## 3. 배지 획득 로직 (Badge Criteria)

| 배지명 | 등급 | 획득 조건 |
| :--- | :--- | :--- |
| **주택 지식인** | Lv.1 | 받은 좋아요 총 20개 이상 |
| **공고 컬렉터** | Lv.1 | 관심 공고 설정 10회 이상 OR 7일 연속 출석 |
| **서울 명예시민**| - | 서울 지역 게시판 활동(글/댓글) 10회 이상 |
| **후기 요정** | Lv.1 | 후기 게시판 게시글 5개 이상 작성 |

## 4. 단계별 구현 계획

### [Step 1] 백엔드 인프라 및 통계 시스템
1. `UserActivityStats` 엔티티 및 Repository 생성.
2. 회원가입 시 `UserActivityStats` 자동 생성 로직 추가.
3. 게시글/댓글/좋아요/관심설정 API 호출 시 통계 수치를 업데이트하는 `Event Listener` 구축.

### [Step 2] 배지 자동 부여 엔진
1. `Badge` 정보 초기화 (Seed Data).
2. 통계 업데이트 시점에 배지 획득 조건을 체크하여 `UserBadge`를 생성하는 서비스 로직 구현.

### [Step 3] 프론트엔드 시각화
1. 게시글/댓글 리스트의 유저 닉네임 옆에 대표 배지 노출.
2. 마이페이지 내 '내 배지함' 뷰 구현 및 활동 통계 가시화.

## 5. 기대 효과
- **신뢰도 형성:** 양질의 정보를 제공하는 유저를 시각적으로 구분 가능.
- **리텐션 증가:** 연속 출석 및 배지 레벨업을 통한 게임화(Gamification) 요소 강화.
- **데이터 축적:** 유저별 관심 지역 및 활동 성향 파악 용이.
