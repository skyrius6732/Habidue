# JPA 확장 개념: QueryDSL (feat. Spring Data JPA의 한계 극복)

## 🎯 학습 목표
Spring Data JPA만으로 해결하기 어려운 복잡한 쿼리 상황에서 QueryDSL과 같은 JPA 확장 기술이 왜 필요한지 이해하고, QueryDSL의 핵심 개념과 장점, 그리고 도입 시점을 파악합니다.

## 💡 1. Spring Data JPA의 편리함과 한계

현재 HabiDue 프로젝트에서는 **Spring Data JPA**를 활용하여 `UserRepository`, `NoticeRepository` 등을 구현했습니다.

*   **편리함:** 메소드 이름만으로 간단한 CRUD 쿼리나 `findBy컬럼명Containing()` 같은 쿼리를 자동 생성해줍니다. `Pageable`을 통해 페이징 기능도 쉽게 사용할 수 있습니다. 개발 속도가 매우 빠릅니다.
*   **한계점:**
    1.  **복잡한 동적 쿼리:** `WHERE` 절의 조건이 유동적으로 변하거나, `if-else` 로직에 따라 쿼리 내용이 달라져야 할 때 (예: 여러 검색 필터를 조합하는 경우).
    2.  **다중 테이블 조인 및 서브 쿼리:** 여러 엔티티를 복잡하게 조인해야 하거나, 서브 쿼리가 필요한 경우.
    3.  **특정 데이터베이스 함수 사용:** `DATE_FORMAT`, `GROUP_CONCAT` 등 특정 DB의 함수를 직접 사용해야 할 때.
    4.  **낮은 쿼리 안정성:** `Repository`의 메소드 이름이 너무 길어지거나 오타가 발생하면 컴파일 시점에는 오류를 알 수 없고, 런타임에 서버를 실행해야만 오류를 발견할 수 있습니다. (런타임 오류)

이런 한계에 부딪혔을 때, JPA의 기능을 더욱 강력하게 확장해주는 기술이 필요합니다. 그중 가장 대표적인 것이 바로 **QueryDSL**입니다.

## 🚀 2. QueryDSL 이란? (핵심 개념)

QueryDSL은 **"정적 타입 기반의 JPQL(JPA Query Language) 빌더"**입니다. 자바 코드를 사용하여 SQL/JPQL과 매우 유사한 형태로 쿼리를 구성할 수 있게 해주는 프레임워크입니다.

*   **정적 타입 기반:**
    *   `QueryDSL`을 사용하면 엔티티 클래스(예: `User`, `Notice`, `Keyword`)를 기반으로 **`Q엔티티`** (예: `QUser`, `QNotice`, `QKeyword`)라는 특별한 클래스가 자동 생성됩니다.
    *   이 `Q클래스`를 사용하면 엔티티의 필드를 마치 객체의 필드처럼 안전하게 참조할 수 있습니다.
*   **JPQL 빌더:**
    *   SQL 쿼리(`SELECT ... FROM ... WHERE ...`)를 문자열로 작성하는 대신, 자바 코드로 `queryFactory.selectFrom(qNotice).where(qNotice.title.contains(keyword)).fetch()` 와 같이 메소드 체이닝 방식으로 조립합니다.

### **QueryDSL의 주요 장점**

1.  **컴파일 시점 오류 감지 (최대 장점!):** 쿼리 메소드나 문자열 쿼리와 달리, QueryDSL은 자바 코드이기 때문에 오타나 잘못된 필드 사용, 잘못된 조인 관계 등 모든 쿼리 관련 오류를 **컴파일 단계에서 즉시 잡아줍니다.** 런타임에 예상치 못한 에러로 서비스가 중단되는 것을 방지합니다.
2.  **강력한 동적 쿼리:** `BooleanBuilder`나 `Where` 절 내에서 조건문을 활용하여, 여러 검색 조건 중 필요한 것들만 유동적으로 쿼리에 포함시키는 것이 매우 쉽고 직관적입니다.
3.  **높은 가독성 및 유지보수성:** SQL과 유사한 문법을 자바 코드로 작성하므로 쿼리의 의미를 파악하기 쉽고, 변경이 용이합니다.
4.  **타입 안정성 보장:** 엔티티 필드들이 `Q클래스`를 통해 자바 타입으로 관리되므로, 타입 캐스팅 오류 등 타입 관련 오류가 발생할 염려가 없습니다.
5.  **IDE의 완벽한 지원:** 자동 완성, 문법 검사, 리팩토링 기능 등을 IDE에서 활용할 수 있어 개발 생산성이 향상됩니다.

## 🚀 3. QueryDSL 사용 예시 (개념적)

```java
// Spring Data JPA
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 메소드 이름이 너무 길어지고, OR 조건이 고정되어 동적 쿼리에 취약
    Page<Notice> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword, Pageable pageable);
}

// QueryDSL을 사용한 Notice 검색 (훨씬 유연하고 동적인 쿼리 가능)
@Service
@RequiredArgsConstructor
public class NoticeQueryRepository { // Custom Repository에 주로 사용

    private final JPAQueryFactory queryFactory; // QueryDSL의 핵심!

    public Page<Notice> searchNoticesByQuerydsl(String titleKeyword, String contentKeyword, String source, Pageable pageable) {
        QNotice notice = QNotice.notice; // Q클래스 사용

        // 조건들을 담을 BooleanBuilder
        BooleanBuilder builder = new BooleanBuilder();

        // 동적 쿼리 예시: titleKeyword가 존재하면 조건 추가
        if (StringUtils.hasText(titleKeyword)) {
            builder.and(notice.title.containsIgnoreCase(titleKeyword));
        }
        // contentKeyword가 존재하면 조건 추가 (OR 조건)
        if (StringUtils.hasText(contentKeyword)) {
            builder.or(notice.content.containsIgnoreCase(contentKeyword));
        }
        // source가 존재하면 조건 추가 (AND 조건)
        if (StringUtils.hasText(source)) {
            builder.and(notice.source.eq(source));
        }
        // 마감일이 오늘 이후인 공고만 (예시)
        builder.and(notice.deadline.after(LocalDateTime.now()));

        List<Notice> content = queryFactory
            .selectFrom(notice)
            .where(builder) // 동적으로 조립된 조건 사용
            .offset(pageable.getOffset()) // 페이징 시작 지점
            .limit(pageable.getPageSize()) // 페이지 크기
            .orderBy(notice.deadline.asc()) // 정렬 조건
            .fetch(); // 쿼리 실행 및 결과 가져오기

        // 전체 개수 (페이징을 위해 필요)
        long total = queryFactory
            .selectFrom(notice)
            .where(builder)
            .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }
}
```
## 📆 4. QueryDSL 도입 시점 (HabiDue 프로젝트 기준)

현재 HabiDue 프로젝트의 쿼리는 아직 복잡하지 않으므로, Spring Data JPA의 쿼리 메소드로 충분히 구현 가능합니다. 하지만 다음과 같은 상황이 발생할 때 QueryDSL 도입을 적극적으로 고려할 수 있습니다.

1.  **검색/필터링 기능 고도화:** 사용자가 여러 조건(키워드, 기간, 출처 등)을 조합하여 공고를 검색하는 기능이 더 복잡해질 때.
2.  **리포팅/통계 기능 추가:** 특정 조건에 따른 공고 집계, 사용자 활동 통계 등 복잡한 조인 및 그룹화 쿼리가 필요할 때.
3.  **N+1 문제의 반복적 발생:** 지연 로딩 때문에 N+1 문제가 자주 발생하여 성능 저하가 나타나고, `Fetch Join`이 필수적인 상황이 많아질 때.
4.  **유지보수성 강화:** 쿼리 관련 런타임 오류를 컴파일 시점에 미리 방지하고 싶은 경우.

**[결론]**
QueryDSL은 Spring Data JPA의 강력한 보완재입니다. HabiDue 프로젝트가 성장하고 쿼리가 복잡해질수록 QueryDSL의 진정한 가치를 느끼게 될 것입니다. 지금 당장은 아니지만, **3주차 크롤러 구현 후 또는 4주차 배포/성능 최적화 시점에 도입을 검토**하는 것을 추천합니다.
