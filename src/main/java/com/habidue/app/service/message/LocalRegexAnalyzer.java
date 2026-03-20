package com.habidue.app.service.message;

import com.habidue.app.dto.message.AiAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class LocalRegexAnalyzer implements MessageAnalyzer {

    // 테스트용 간단한 욕설 리스트 (실제 운영 시에는 더 확장 필요)
    private static final List<String> BANNED_WORDS = Arrays.asList("바보", "멍청이", "나쁜놈");

    @Override
    public AiAnalysisResult analyze(String content) {
        for (String word : BANNED_WORDS) {
            if (content.contains(word)) {
                return AiAnalysisResult.builder()
                        .score(0.8)
                        .reason("금지어 포함: " + word)
                        .shouldBlock(false) // 단순 욕설은 경고만
                        .build();
            }
        }
        return AiAnalysisResult.safe();
    }

    @Override
    public String getName() {
        return "LOCAL_REGEX";
    }
}
