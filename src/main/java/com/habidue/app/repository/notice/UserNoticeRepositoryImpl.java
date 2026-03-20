package com.habidue.app.repository.notice;

import com.habidue.app.domain.user.User;
import com.habidue.app.domain.usernotice.QUserNotice;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserNoticeRepositoryImpl implements UserNoticeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, Long> countInterestBulk(List<Long> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QUserNotice userNotice = QUserNotice.userNotice;

        // SELECT notice.id, COUNT(*) FROM user_notices WHERE notice.id IN (...) GROUP BY notice.id
        List<Tuple> result = queryFactory
                .select(userNotice.notice.id, userNotice.count())
                .from(userNotice)
                .where(userNotice.notice.id.in(noticeIds))
                .groupBy(userNotice.notice.id)
                .fetch();

        return result.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(userNotice.notice.id),
                        tuple -> tuple.get(userNotice.count())
                ));
    }

    @Override
    public Set<Long> findFavoriteIds(User user, List<Long> noticeIds) {
        if (user == null || noticeIds == null || noticeIds.isEmpty()) {
            return Collections.emptySet();
        }

        QUserNotice userNotice = QUserNotice.userNotice;

        // SELECT notice.id FROM user_notices WHERE user = :user AND notice.id IN (...)
        List<Long> result = queryFactory
                .select(userNotice.notice.id)
                .from(userNotice)
                .where(userNotice.user.eq(user)
                        .and(userNotice.notice.id.in(noticeIds)))
                .fetch();

        return result.stream().collect(Collectors.toSet());
    }
}
