package com.habidue.app.service.message;

import com.habidue.app.dto.message.AiAnalysisResult;

public interface MessageAnalyzer {
    /**
     * 메시지 내용을 분석하여 위험도와 사유를 반환합니다.
     */
    AiAnalysisResult analyze(String content);

    /**
     * 분석기 이름 (설정에서 선택 가능하게 하기 위함)
     */
    String getName();
}
