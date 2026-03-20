package com.habidue.app.repository.message;

import com.habidue.app.domain.message.Message;
import com.habidue.app.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 수신함 조회 (삭제하지 않은 쪽지만)
    Page<Message> findByReceiverAndDeletedByReceiverFalseOrderByCreatedAtDesc(User receiver, Pageable pageable);

    // 발신함 조회 (삭제하지 않은 쪽지만)
    Page<Message> findBySenderAndDeletedBySenderFalseOrderByCreatedAtDesc(User sender, Pageable pageable);

    // 안읽은 쪽지 수
    long countByReceiverAndIsReadFalseAndDeletedByReceiverFalse(User receiver);

    // 신고된 쪽지 조회 (관리자용)
    Page<Message> findByIsReportedTrueOrderByCreatedAtDesc(Pageable pageable);

    // 특정 유저에게 온 AI 위험 메시지 조회 (관리자용)
    @Query("SELECT m FROM Message m WHERE m.aiScore >= :threshold ORDER BY m.createdAt DESC")
    Page<Message> findHighRiskMessages(@Param("threshold") Double threshold, Pageable pageable);
}
