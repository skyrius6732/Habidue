package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Report;
import com.habidue.app.domain.board.ReportStatus;
import com.habidue.app.domain.board.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long>, ReportRepositoryCustom {
    List<Report> findAllByTargetIdAndTargetType(Long targetId, ReportTargetType targetType);
    
    // [시니어 조치] 중복 신고 방지를 위한 체크 메서드
    boolean existsByReporter_IdAndTargetIdAndTargetType(Long reporterId, Long targetId, com.habidue.app.domain.board.ReportTargetType targetType);
}
