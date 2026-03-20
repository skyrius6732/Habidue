package com.habidue.app.repository.tag;

import com.habidue.app.domain.tag.Tag;
import com.habidue.app.domain.tag.TagType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    Optional<Tag> findByNameAndType(String name, TagType type);
    List<Tag> findByNameContaining(String name);

    /**
     * [시니어 조치] 실제 게시글(PostTag)에 가장 많이 사용된 태그 상위 10개를 가져옴
     * 양방향 연관관계(t.postTags)를 활용한 정석적인 객체 그래프 탐색 쿼리
     */
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Tag t LEFT JOIN t.postTags pt GROUP BY t ORDER BY COUNT(pt) DESC")
    List<Tag> findTopTags(org.springframework.data.domain.Pageable pageable);
}
