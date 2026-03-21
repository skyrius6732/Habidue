package com.habidue.app.repository.message;

import com.habidue.app.domain.message.Message;
import com.habidue.app.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 수신함 조회 (삭제하지 않은 쪽지만)
    Page<Message> findByReceiverAndDeletedByReceiverFalseOrderByCreatedAtDesc(User receiver, Pageable pageable);

    // 발신함 조회 (삭제하지 않은 쪽지만)
    Page<Message> findBySenderAndDeletedBySenderFalseOrderByCreatedAtDesc(User sender, Pageable pageable);

    // 안읽은 쪽지 수
    long countByReceiverAndIsReadFalseAndDeletedByReceiverFalse(User receiver);

    // 신고된 쪽지 조회 (관리자용)
    Page<Message> findByIsReportedTrueOrderByCreatedAtDesc(Pageable pageable);

    // 특정 상대방이 보낸 메시지 중 내가 읽지 않은 메시지 수 (삭제하지 않은 것만)
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.sender = :partner AND m.receiver = :user " +
           "AND m.isRead = false AND m.deletedByReceiver = false")
    long countUnreadMessagesWithPartner(@Param("user") User user, @Param("partner") User partner);

    // 대화방 목록 조회 (상대방별 최신 쪽지 1개씩, 최근 90일 내역만)
    @Query("SELECT m FROM Message m WHERE m.id IN (" +
           "  SELECT MAX(m2.id) FROM Message m2 " +
           "  WHERE ((m2.sender = :user AND m2.deletedBySender = false) " +
           "  OR (m2.receiver = :user AND m2.deletedByReceiver = false)) " +
           "  AND m2.createdAt >= :since " +
           "  GROUP BY CASE WHEN m2.sender = :user THEN m2.receiver.id ELSE COALESCE(m2.sender.id, 0) END" +
           ") ORDER BY m.createdAt DESC")
    Page<Message> findLatestMessagesByPartnersWithTime(@Param("user") User user, @Param("since") java.time.LocalDateTime since, Pageable pageable);

    // 특정 유저와의 전체 대화 내역 조회 (최근 90일)
    @Query("SELECT m FROM Message m " +
           "WHERE ((m.sender = :user AND m.receiver = :partner AND m.deletedBySender = false) " +
           "OR (m.sender = :partner AND m.receiver = :user AND m.deletedByReceiver = false)) " +
           "AND m.createdAt >= :since " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversationWithPartnerWithTime(@Param("user") User user, @Param("partner") User partner, @Param("since") java.time.LocalDateTime since);

    // 특정 유저와의 전체 대화 내역 조회 (삭제용)
    @Query("SELECT m FROM Message m " +
           "WHERE (m.sender = :user AND m.receiver = :partner) " +
           "OR (m.sender = :partner AND m.receiver = :user)")
    List<Message> findConversationWithPartner(@Param("user") User user, @Param("partner") User partner);

    // 특정 유저에게 온 AI 위험 메시지 조회 (관리자용)
    @Query("SELECT m FROM Message m WHERE m.aiScore >= :threshold ORDER BY m.createdAt DESC")
    Page<Message> findHighRiskMessages(@Param("threshold") Double threshold, Pageable pageable);
}
