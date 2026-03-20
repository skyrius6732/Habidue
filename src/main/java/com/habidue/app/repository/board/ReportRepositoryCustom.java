package com.habidue.app.repository.board;

import com.habidue.app.domain.board.ReportTargetType;
import com.habidue.app.dto.admin.ReportAdminResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportRepositoryCustom {
    Page<ReportAdminResponseDto> findGroupedReports(ReportTargetType targetType, String status, String keyword, Pageable pageable);
}
