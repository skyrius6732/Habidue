package com.habidue.app.dto.message;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class AiAnalysisResult {
    private final Double score; // 0.0 ~ 1.0 (위험도)
    private final String reason; // 분석 사유
    private final boolean shouldBlock; // 즉시 차단 여부 (예: 심각한 욕설)

    public static AiAnalysisResult safe() {
        return new AiAnalysisResult(0.0, "안전함", false);
    }
}
