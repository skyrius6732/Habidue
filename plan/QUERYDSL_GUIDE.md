# QueryDSL 핵심 개념 및 워크플로우

## 1. QueryDSL이란?
QueryDSL은 자바 코드로 SQL 스타일의 쿼리를 작성할 수 있게 해주는 프레임워크입니다. JPQL을 문자열이 아닌 **Type-Safe**한 코드로 작성하여 컴파일 시점에 오류를 잡을 수 있는 장점이 있습니다.

## 2. Q-Class (Q파일)의 정의
- **정의**: 엔티티 클래스의 메타 정보를 담고 있는 클래스입니다. (예: `Notice` -> `QNotice`)
- **생성**: 프로젝트 빌드 시 `APT(Annotation Processing Tool)`가 엔티티 클래스를 분석하여 자동으로 생성합니다.
- **역할**: 쿼리를 작성할 때 테이블의 컬럼을 자바 필드처럼 참조할 수 있게 해주어 오타를 방지합니다.

## 3. 전체적인 흐름
1. **Entity 작성**: JPA 엔티티(`Notice.java`)를 작성합니다.
2. **Compile**: `./gradlew compileJava` 수행 시 `QNotice.java`가 생성됩니다.
3. **Repository 확장**: `CustomRepository` 인터페이스와 그 구현체(`Impl`)를 생성합니다.
4. **JPAQueryFactory**: 구현체에서 `JPAQueryFactory`를 사용하여 동적 쿼리를 작성합니다.
5. **동적 정렬/필터링**: `BooleanExpression`이나 `OrderSpecifier`를 사용하여 런타임에 정렬 조건(알림순, 관심순 등)을 변경합니다.

## 4. HabiDue 적용 사례
- **알림 키워드순 정렬**: 사용자의 키워드 목록을 가져와 제목에 포함된 개수를 QueryDSL의 `NumberExpression`으로 계산하여 정렬에 반영했습니다.
- **관심 공고순 정렬**: `UserNotice` 테이블과의 조인 횟수를 카운트하여 인기 있는 공고를 상단에 노출하는 복잡한 쿼리를 구현했습니다.
