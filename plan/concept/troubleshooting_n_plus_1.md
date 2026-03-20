# [Troubleshooting] JPA N+1 문제 정의와 실전 해결 전략 (habiDue 사례)

이 문서는 `habiDue` 프로젝트의 공고 목록 조회 API에서 발생한 성능 저하 원인인 **N+1 문제**를 정의하고, 이를 어떻게 최신 트렌드에 맞춰 해결했는지 강사의 입장에서 설명합니다.

---

## 1. N+1 문제란 무엇인가? (개념 이해)

### 🍕 피자 주문 비유
여러분에게 10명의 친구가 있고, 각자 어떤 피자를 먹고 싶은지 물어본다고 가정해봅시다.

*   **정상적인 방법 (1번의 주문)**: "얘들아, 먹고 싶은 피자 다 말해봐!"라고 한 번에 물어본 뒤, 피자 가게에 전화를 한 번 걸어 10판을 주문합니다. (**쿼리 1번**)
*   **N+1 문제 (N+1번의 주문)**: 
    1. 친구 10명의 명단을 가져옵니다. (**1번의 작업**)
    2. 1번 친구에게 묻고 피자 가게에 전화합니다. (**전화 1번**)
    3. 2번 친구에게 묻고 피자 가게에 전화합니다. (**전화 1번**)
    4. ... 10번 반복합니다. (**전화 10번**)
    
결과적으로 친구 명단을 가져오는 작업 **1번**과, 각 친구마다 발생하는 **N번**의 추가 작업이 합쳐져 총 **N+1번**의 비효율적인 통신이 발생합니다. 이것이 바로 DB 성능을 갉아먹는 **N+1 문제**입니다.

---

## 2. 우리 프로젝트의 문제 상황 (Before)

`NoticeController`에서 공고 목록(Page)을 가져올 때 다음과 같은 코드가 있었습니다.

```java
// 1. 공고 10개를 가져온다 (쿼리 1번)
Page<Notice> noticePage = noticeService.getNotices(pageable);

// 2. 루프를 돌며 각 공고마다 추가 정보를 조회한다 (10번 반복)
Page<NoticeResponseDto> dtoPage = noticePage.map(notice -> {
    // [쿼리 N-1] 태그 조회
    List<String> tagNames = noticeTagRepository.findAllByNotice(notice)...;
    
    // [쿼리 N-2] 하트 개수 조회
    dto.setInterestCount(userNoticeRepository.countByNotice(notice));
    
    // [쿼리 N-3] 나의 좋아요 여부 조회
    dto.setFavorite(userNoticeRepository.existsByUserAndNotice(currentUser, notice));
    
    return dto;
});
```

*   **결과**: 공고 10개를 보여주는데 쿼리가 무려 **31번**(1 + 10*3) 발생! 
*   데이터가 1,000개라면? 쿼리가 **3,001번** 발생하여 서버가 매우 느려지게 됩니다.

---

## 3. 해결책 1: 컬렉션 조회의 구원자 `@BatchSize`

### 왜 `fetch join`을 안 썼나요?
`fetch join`은 한 번에 다 가져오니까 좋지만, **페이징(Pagination)**과 함께 쓰면 위험합니다. 
DB에서 10개만 끊어서 가져와야 하는데, 1:N 조인을 하면 데이터가 뻥튀기되어 하이버네이트가 "안 되겠다, 내가 메모리로 다 읽어서 직접 계산할게!"라며 DB의 모든 데이터를 서버 메모리로 올려버립니다. (메모리 초과 위험)

### 해결: `@BatchSize`
그래서 우리는 `Notice` 엔티티의 태그 리스트에 `@BatchSize(size = 100)`를 붙였습니다.

*   **효과**: 공고 10개의 태그를 조회할 때, "공고 1번 태그 줘, 2번 태그 줘..."가 아니라 **"공고 1, 2, 3... 10번의 태그들을 한꺼번에 줘!"**라는 `WHERE notice_id IN (1, 2, ..., 10)` 쿼리가 1번 나갑니다.
*   **성능**: 10번 나가던 쿼리가 **1번**으로 줄어듭니다.

---

## 4. 해결책 2: QueryDSL Bulk Fetching & Memory Mapping

통계 데이터(`count`)나 존재 여부(`exists`)는 엔티티 연관관계로 풀기보다 **직접 한꺼번에 조회해서 메모리에서 매핑**하는 것이 가장 효율적입니다.

### 단계 1: 한 번에 묶어서 가져오기 (Bulk Fetch)
QueryDSL을 사용해 현재 페이지에 있는 모든 공고 ID를 `IN` 절로 넣어 필요한 데이터를 한 번에 가져옵니다.

```java
// 모든 공고의 하트 수를 ID별로 그룹화해서 한 번에 가져옴 (Map 형태)
Map<Long, Long> interestCounts = userNoticeRepository.countInterestBulk(noticeIds);

// 내가 좋아요를 누른 공고 ID들만 싹 다 가져옴 (Set 형태)
Set<Long> favoriteIds = userNoticeRepository.findFavoriteIds(currentUser, noticeIds);
```

### 단계 2: 메모리에서 짝짓기 (Mapping)
DB를 다시 부르는 대신, 미리 가져온 `Map`과 `Set`에서 데이터를 꺼내 씁니다.

```java
dto.setInterestCount(interestCounts.getOrDefault(notice.getId(), 0L)); // 메모리에서 찾기
dto.setFavorite(favoriteIds.contains(notice.getId())); // 메모리에서 찾기
```

---

## 5. 최종 결과 (After)

| 구분 | 수정 전 (N=10 기준) | 수정 후 (고정) | 비고 |
| :--- | :--- | :--- | :--- |
| **공고 목록 조회** | 1회 | 1회 | 기본 쿼리 |
| **태그 목록 조회** | 10회 | **1회** | `@BatchSize` 적용 |
| **하트 개수 조회** | 10회 | **1회** | QueryDSL Bulk & Map |
| **좋아요 여부 확인** | 10회 | **1회** | QueryDSL Bulk & Set |
| **총 쿼리 수** | **31회** | **4회** | **약 87% 감소** |

### 요약: 언제 무엇을 쓸까?
1.  **N:1 관계**: 무조건 `fetch join` (페이징 영향 없음)
2.  **1:N 관계 (컬렉션)**: `@BatchSize` (페이징 안전)
3.  **복잡한 계산/상태**: Bulk 조회 후 메모리 `Map` 매핑 (가장 빠름)

이 원칙만 지키면 JPA를 사용하면서 성능 문제로 고민할 일은 거의 없을 것입니다!
