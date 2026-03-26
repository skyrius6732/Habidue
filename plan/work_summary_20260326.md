# 2026-03-26 작업 요약 (Work Summary)

## 1. 관리자 커뮤니티 관리 화면 오류 수정
- **현상:** `AdminCommunityView.vue` 접속 시 `ReferenceError: editForm is not defined` 발생.
- **원인:** 이전 작업 과정에서 필수 상태 변수(`editForm`, `isEditModalOpen` 등) 선언부 누락.
- **조치:** 
    - 누락된 `ref` 선언 및 초기화 완료.
    - 스크립트 내 변수 선언 순서 최적화 (모든 상태값을 상단으로 집결)하여 참조 안정성 확보.

## 2. 보상 시뮬레이션 연구소 (Reward Lab) 구축
- **파일:** 
    - Backend: `AdminSimulationController.java`, `SimulationUserStatsDto.java`, `SimulationResponseDto.java`
    - Frontend: `AdminSimulationView.vue`
- **핵심 아키텍처:**
    - **Performer Context:** `SecurityContextHolder`를 시뮬레이션 대상자로 임시 교체하여 실제 서비스 로직(`PostService`, `CommentService`)을 100% 동일하게 실행.
    - **Dual User Tracking:** 보상 수신자(Subject)와 액션 수행자(Actor)의 지표를 동시에 추적하여 상호작용 보상 검증 가능.
    - **Cumulative Table:** 시뮬레이션 시작 시점의 `Initial` 값을 보존하여 총 누적 증감을 실시간 계산.

## 3. 게이미피케이션 로직 정밀 고도화
- **카르마(신뢰 점수) 상한선 보존 로직:**
    - 좋아요가 상한선(10개/1.0P)을 초과한 상태에서 삭제 발생 시, 잔여 좋아요가 상한선을 충족하면 기존 점수 유지.
    - `POST_ID` 단위로 카르마 회수 키를 통일하여 게시글/댓글/답글 보상을 통합 관리.
- **데이터 정합성 및 멱등성 확보:**
    - 게시글 삭제 시 하위 활동 연쇄 회수 과정에서 `ACTIVE` 상태인 데이터만 정산하도록 필터링 강화.
    - `UserActivityStats` 엔티티 내 모든 차감 메서드에 `Math.max(0, ...)` 안전장치 적용.
- **삭제 상태값 세분화:**
    - `USER_DELETED`: 사용자에 의한 삭제 ("작성자에 의해 삭제된 댓글입니다.")
    - `DELETED`: 운영 정책에 의한 삭제 ("운영 정책 위반으로 삭제된 댓글입니다.")

## 4. 미결 과제 및 향후 계획
- **지표 미세 오차 정밀 추적:** 마구잡이 액션 반복 시 발생하는 EXP/총댓글 수의 미세한 싱크 오차에 대해 `ExpHistory` 이력 기반의 완전 재계산 로직 검토 필요.
- **시뮬레이션 로그 고도화:** 현재 프론트엔드 메모리에만 쌓이는 로그를 더 정교하게 시각화하여 데이터 튐 현상 감시 강화.
- **사진 보너스 중복 체크:** 카테고리 변경이나 수정 시 사진 보너스가 중복 부여되거나 누락되는 케이스 점검 필요.
