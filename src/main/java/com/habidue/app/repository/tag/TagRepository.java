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
     * [시니어 조치] 실제 게시글(PostTag)에 사용된 태그 중 빈도수 상위 10개를 가져옴
     * JOIN(Inner Join)을 사용하여 최소 1회 이상 사용된 태그만 선별
     */
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Tag t JOIN t.postTags pt GROUP BY t ORDER BY COUNT(pt) DESC")
    List<Tag> findTopTags(org.springframework.data.domain.Pageable pageable);
}
