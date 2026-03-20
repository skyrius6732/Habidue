package com.habidue.app.repository.notice;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.tag.NoticeTag;
import com.habidue.app.domain.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeTagRepository extends JpaRepository<NoticeTag, Long> {
    Optional<NoticeTag> findByNoticeAndTag(Notice notice, Tag tag);
    List<NoticeTag> findAllByNotice(Notice notice);

    @Query("SELECT nt FROM NoticeTag nt JOIN FETCH nt.tag WHERE nt.notice = :notice")
    List<NoticeTag> findAllByNoticeWithTag(@Param("notice") Notice notice);

    @Modifying
    @Query("DELETE FROM NoticeTag nt WHERE nt.notice = :notice")
    void deleteByNoticeBulk(@Param("notice") Notice notice);

    void deleteAllByNotice(Notice notice);
}
