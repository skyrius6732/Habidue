package com.habidue.app.repository.notice;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.usernotice.UserNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserNoticeRepository extends JpaRepository<UserNotice, Long>, UserNoticeRepositoryCustom {
    // 객체 기반 메소드
    Page<UserNotice> findByUser(User user, Pageable pageable);
    java.util.List<UserNotice> findAllByUser(User user); // 추가
    Optional<UserNotice> findByUserAndNotice(User user, Notice notice);
    boolean existsByUserAndNotice(User user, Notice notice);
    long countByNotice(Notice notice);

    // ID 기반 메소드 (Service 계층 호환용 추가)
    boolean existsByUserIdAndNoticeId(Long userId, Long noticeId);

    @org.springframework.data.jpa.repository.Query("SELECT un FROM UserNotice un " +
           "JOIN FETCH un.notice n " +
           "LEFT JOIN FETCH n.noticeTags nt " +
           "LEFT JOIN FETCH nt.tag " +
           "WHERE un.user.id = :userId")
    Page<UserNotice> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId, Pageable pageable);
}
