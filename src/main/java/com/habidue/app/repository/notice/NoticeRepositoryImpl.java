package com.habidue.app.repository.notice;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.domain.notice.QNotice;
import com.habidue.app.domain.tag.QNoticeTag;
import com.habidue.app.domain.tag.QTag;
import com.habidue.app.domain.usernotice.QUserNotice;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.habidue.app.domain.user.User;

@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public long incrementInterestCount(Long noticeId, int threshold) {
        QNotice notice = QNotice.notice;

        // 원자적 업데이트: interestCount + 1 & 조건부 isBoardActive 활성화 및 lastPostAt 초기화
        long count = queryFactory.update(notice)
                .set(notice.interestCount, notice.interestCount.add(1))
                .set(notice.isBoardActive, new CaseBuilder()
                        .when(notice.interestCount.add(1).goe(threshold))
                        .then(true)
                        .otherwise(notice.isBoardActive))
                .set(notice.lastPostAt, new CaseBuilder()
                        .when(notice.interestCount.add(1).goe(threshold).and(notice.lastPostAt.isNull()))
                        .then(LocalDateTime.now())
                        .otherwise(notice.lastPostAt))
                .where(notice.id.eq(noticeId))
                .execute();

        return count;
    }

    @Override
    public long decrementInterestCount(Long noticeId) {
        QNotice notice = QNotice.notice;

        long count = queryFactory.update(notice)
                .set(notice.interestCount, new CaseBuilder()
                        .when(notice.interestCount.gt(0))
                        .then(notice.interestCount.subtract(1))
                        .otherwise(0))
                .where(notice.id.eq(noticeId))
                .execute();

        return count;
    }

    @Override
    public void updateStatus(Long noticeId, NoticeStatus status) {
        QNotice notice = QNotice.notice;
        queryFactory.update(notice)
                .set(notice.status, status)
                .where(notice.id.eq(noticeId))
                .execute();
    }

    @Override
    public Page<Notice> searchNotices(String keyword, List<String> sources, List<String> statuses, String sortOrder, List<String> userKeywords, User currentUser, boolean showOnlyFuture, Boolean isBoardActive, Boolean isNew, Pageable pageable) {
        QNotice notice = QNotice.notice;
        QUserNotice userNotice = QUserNotice.userNotice;
        QNoticeTag noticeTag = QNoticeTag.noticeTag;
        QTag tag = QTag.tag;

        BooleanBuilder builder = new BooleanBuilder();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime activityThreshold = now.minusDays(7);
        
        // [시니어 조치] 최근 48시간 이내 등록 필터 (isNew=true) - 방문 주기 고려 확장
        if (isNew != null && isNew) {
            builder.and(notice.createdAt.goe(now.minusHours(48)));
        }
        
        // [시니어 조치] 해금된 공고 전용 필터 적용 (필드값 OR 관심유저 10명 이상 OR 깨우기 성공 및 활동중)
        if (isBoardActive != null && isBoardActive) {
            BooleanBuilder activeBuilder = new BooleanBuilder();
            activeBuilder.or(notice.isBoardActive.eq(true));
            activeBuilder.or(notice.interestCount.goe(10));
            
            // 깨우기 성공 조건: revivedAt이 있고 활동성이 유지되는 경우
            activeBuilder.or(notice.revivedAt.isNotNull().and(
                notice.revivedAt.goe(activityThreshold).or(notice.lastPostAt.goe(activityThreshold))
            ));
            
            builder.and(activeBuilder);
        } else if (isBoardActive != null && !isBoardActive) {
            // [중요] '잠긴' 소통방은 위 모든 활성화 조건을 만족하지 '않아야' 함
            builder.and(notice.isBoardActive.eq(false));
            builder.and(notice.interestCount.lt(10));
            
            // 깨우기 성공 상태가 아니어야 함
            builder.and(notice.revivedAt.isNull().or(
                notice.revivedAt.lt(activityThreshold).and(notice.lastPostAt.lt(activityThreshold))
            ));
            
            // [시니어 조치] '오픈 준비 중' 탭에서는 이미 마감된 공고는 제외 (현재 접수 중이거나 예정인 것만)
            builder.and(notice.deadline.goe(now).or(notice.deadline.isNull()));
        }

        // 0. 기본 노출 기간 및 예외 정책 적용
        BooleanBuilder visibilityBuilder = new BooleanBuilder();
        
        // (1) 최근 1년 이내의 신선한 공고 (기본)
        visibilityBuilder.or(notice.announcementDate.isNotNull().and(notice.announcementDate.goe(now.minusYears(1))));
        visibilityBuilder.or(notice.announcementDate.isNull().and(notice.createdAt.goe(now.minusYears(1))));
        
        // (2) 이미 활성화된 소통방 (1년이 지났어도 보존)
        visibilityBuilder.or(notice.isBoardActive.eq(true));
        
        // (3) 내가 찜한 공고 (1년이 지났어도 보존)
        if (currentUser != null) {
            visibilityBuilder.or(notice.id.in(
                JPAExpressions.select(userNotice.notice.id)
                    .from(userNotice)
                    .where(userNotice.user.id.eq(currentUser.getId()))
            ));
        }

        // (4) 관리자는 모든 공고 조회 가능
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            builder.and(visibilityBuilder);
        }

        // 1. 검색 필터
        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(notice.title.containsIgnoreCase(keyword)
                    .or(notice.content.containsIgnoreCase(keyword)));
        }

        // 2. 기관 필터 (IN)
        if (sources != null && !sources.isEmpty()) {
            builder.and(notice.source.in(sources));
        }

        // 3. 상태 필터 (DB 필드 + 태그 기반 통합)
        if (statuses != null && !statuses.isEmpty()) {
            List<NoticeStatus> statusEnums = new ArrayList<>();
            List<String> targetTagNames = new ArrayList<>();
            
            for (String s : statuses) {
                try {
                    statusEnums.add(NoticeStatus.valueOf(s));
                } catch (IllegalArgumentException ignored) {}
                
                // 태그 매핑 (데이터 리셋 후의 정합성 보완)
                if ("RECRUITING".equals(s)) targetTagNames.add("접수중");
                else if ("CLOSED".equals(s)) {
                    targetTagNames.add("마감");
                    targetTagNames.add("발표완료");
                    targetTagNames.add("이전안내");
                }
                else if ("RESULT".equals(s)) targetTagNames.add("결과발표");
                else if ("INFO".equals(s)) targetTagNames.add("안내");
            }

            BooleanBuilder statusOrBuilder = new BooleanBuilder();
            if (!statusEnums.isEmpty()) {
                statusOrBuilder.or(notice.status.in(statusEnums));
            }
            if (!targetTagNames.isEmpty()) {
                statusOrBuilder.or(notice.id.in(
                    JPAExpressions.select(noticeTag.notice.id)
                        .from(noticeTag)
                        .join(noticeTag.tag, tag)
                        .where(tag.name.in(targetTagNames).and(tag.type.eq(com.habidue.app.domain.tag.TagType.SYSTEM)))
                ));
            }
            builder.and(statusOrBuilder);
        }

        // 4. [추가] 오늘 이후 공고만 보기 필터
        if (showOnlyFuture) {
            // 마감일이 미래이거나, 마감일 정보가 없는데 공고일이 오늘 이후인 경우
            builder.and(notice.deadline.goe(now).or(notice.deadline.isNull().and(notice.announcementDate.goe(now.minusDays(1)))));
        }

        // 4. 관심 공고 필터링 제거 (이제 모든 사용자의 하트 수를 기준으로 정렬만 수행)
        // 기존에 있던 'interest' 관련 builder.and() 로직을 삭제했습니다.

        // --- 정렬 로직 고도화 (서브쿼리 방식) ---
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        
        if ("alarm".equals(sortOrder) && userKeywords != null && !userKeywords.isEmpty()) {
            // [알림 키워드 순] 서브쿼리를 정렬 조건에 직접 사용
            QNoticeTag subNT = new QNoticeTag("subNT");
            QTag subT = new QTag("subT");
            
            orders.add(new OrderSpecifier<>(Order.DESC, JPAExpressions
                    .select(subT.count())
                    .from(subNT)
                    .join(subNT.tag, subT)
                    .where(subNT.notice.eq(notice).and(subT.name.in(userKeywords)))));
            
            orders.add(notice.announcementDate.desc().nullsLast());
        } else if ("interest".equals(sortOrder) || (isBoardActive != null && !isBoardActive)) {
            // [최적화] 소통방 '오픈 준비 중' 탭이거나 '관심도 순' 선택 시: 하트 수 -> 마감 임박 순 정렬
            orders.add(notice.interestCount.desc());
            orders.add(notice.deadline.asc().nullsLast()); // 마감 임박한 것부터 우선 노출
            orders.add(notice.announcementDate.desc().nullsLast());
        } else if ("deadline".equals(sortOrder)) {
            orders.add(new CaseBuilder().when(notice.deadline.goe(now)).then(0).otherwise(1).asc());
            orders.add(notice.deadline.asc());
        } else {
            // [시니어 조치] 최신순 정렬 시 공고 게시일(announcementDate)을 최우선으로 하여
            // 대량 수집 시에도 실제 공고 날짜 순으로 정렬되도록 보정
            orders.add(notice.announcementDate.desc().nullsLast());
            orders.add(notice.createdAt.desc());
        }

        JPAQuery<Notice> query = queryFactory
                .selectFrom(notice)
                .leftJoin(notice.noticeTags, noticeTag).fetchJoin() // [시니어 조치] 태그 페치 조인
                .leftJoin(noticeTag.tag, tag).fetchJoin() // [시니어 조치] 실제 태그 엔티티 페치 조인
                .where(builder)
                .distinct(); // [시니어 조치] FetchJoin으로 인한 중복 데이터 제거 필수!

        for (OrderSpecifier<?> order : orders) {
            query.orderBy(order);
        }

        List<Notice> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회 (필터가 적용된 상태에서 정확한 전체 카운트)
        Long total = queryFactory
                .select(notice.id.countDistinct())
                .from(notice)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
