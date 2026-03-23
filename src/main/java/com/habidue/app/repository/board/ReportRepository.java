package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Report;
import com.habidue.app.domain.board.ReportStatus;
import com.habidue.app.domain.board.ReportTargetType;
import com.habidue.app.domain.user.User; // 추가
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long>, ReportRepositoryCustom {
    List<Report> findAllByTargetIdAndTargetType(Long targetId, ReportTargetType targetType);
    
    // [시니어 조치] 중복 신고 방지를 위한 체크 메서드
    boolean existsByReporter_IdAndTargetIdAndTargetType(Long reporterId, Long targetId, com.habidue.app.domain.board.ReportTargetType targetType);

    // [시니어 조치] 특정 신고자가 해당 대화방에 대해 이미 신고했는지 확인 (삭제된 메시지에 대한 신고는 무시)
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) > 0 FROM Report r, Message m " +
           "WHERE r.targetId = m.id AND r.targetType = 'MESSAGE' AND r.status = 'WAITING' " +
           "AND m.isDeleted = false " +
           "AND r.reporter.id = :reporterId " +
           "AND ((m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1))")
    boolean existsActiveReportByConversation(@org.springframework.data.repository.query.Param("reporterId") Long reporterId, @org.springframework.data.repository.query.Param("user1") User user1, @org.springframework.data.repository.query.Param("user2") User user2);
}
