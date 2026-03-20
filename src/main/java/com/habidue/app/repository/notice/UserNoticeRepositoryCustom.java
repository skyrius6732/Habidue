package com.habidue.app.repository.notice;

import com.habidue.app.domain.user.User;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserNoticeRepositoryCustom {
    // 공고 ID 목록을 받아 각 공고별 하트(좋아요) 개수를 Map(ID, Count)으로 반환
    Map<Long, Long> countInterestBulk(List<Long> noticeIds);
    
    // 특정 사용자가 좋아요를 누른 공고 ID 목록을 Set으로 반환
    Set<Long> findFavoriteIds(User user, List<Long> noticeIds);
}
