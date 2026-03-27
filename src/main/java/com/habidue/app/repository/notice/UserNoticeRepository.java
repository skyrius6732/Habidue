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
    java.util.List<UserNotice> findAllByNotice(Notice notice); // 추가
    Optional<UserNotice> findByUserAndNotice(User user, Notice notice);
    boolean existsByUserAndNotice(User user, Notice notice);
    long countByNotice(Notice notice);

    // ID 기반 메소드 (Service 계층 호환용 추가)
    boolean existsByUserIdAndNoticeId(Long userId, Long noticeId);

    @org.springframework.data.jpa.repository.Query("SELECT un FROM UserNotice un " +
           "JOIN FETCH un.notice n " +
           "WHERE un.user.id = :userId")
    Page<UserNotice> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT un FROM UserNotice un " +
           "JOIN FETCH un.notice n " +
           "WHERE un.user = :user")
    java.util.List<UserNotice> findAllByUserWithNotice(@org.springframework.data.repository.query.Param("user") User user);

    /**
     * 마감 기한이 하루 남은 관심 공고 목록을 조회함 (알림용)
     * - notice.deadline 이 내일이거나, 사용자가 수동으로 설정한 userDeadline 이 내일인 경우
     */
    @org.springframework.data.jpa.repository.Query("SELECT un FROM UserNotice un " +
            "JOIN FETCH un.user u " +
            "JOIN FETCH un.notice n " +
            "WHERE (n.deadline BETWEEN :start AND :end) " +
            "OR (un.userDeadline BETWEEN :start AND :end)")
    java.util.List<UserNotice> findDeadlineImminentNotices(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, 
                                                         @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    void deleteByUser(User user);
}
