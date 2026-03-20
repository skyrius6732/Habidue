package com.habidue.app.domain.board;

public enum ReportStatus {
    WAITING,        // 처리 대기
    BLIND_COMPLETE, // 블라인드 완료
    DELETE_COMPLETE, // 영구 삭제 완료
    REJECTED        // 반려 (무시)
}
