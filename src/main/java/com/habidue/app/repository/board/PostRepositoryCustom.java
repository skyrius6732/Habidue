package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostRepositoryCustom {
    // 타입별, 카테고리별, 공고별, 상태별 게시글 조회 (Fetch Join 적용)
    Page<Post> findPosts(PostType type, Long noticeId, String category, String subCategory, String keyword, String status, String regionTag, String tagName, Pageable pageable, Long currentUserId, boolean isAdmin);

    // 작성자 ID 기준 게시글 조회 (Fetch Join 적용)
    Page<Post> findPostsByAuthor(Long authorId, String keyword, String status, Pageable pageable);

    // 공고 상세 미리보기용 인기글 TOP 5 추출
    List<Post> findTopByNoticeIdOrderByPopularity(Long noticeId, int limit);

    // [시니어 조치] 커뮤니티 전체 실시간 인기글 조회 (최근 24시간, 가중치 적용)
    List<Post> findTrendingPosts(int limit);

    // [시니어 조치] 카테고리별 검색 결과 카운트용
    long countByKeywordAndType(String keyword, PostType type);
}
