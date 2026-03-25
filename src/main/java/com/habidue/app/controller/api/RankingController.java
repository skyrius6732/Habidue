package com.habidue.app.controller.api;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.ranking.HotNoticeResponseDto;
import com.habidue.app.dto.ranking.RankerResponseDto;
import com.habidue.app.service.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * 기간별 및 분야별 활동 랭킹 조회
     * @param period "DAILY", "WEEKLY", "MONTHLY", "ALL" (기본값 "WEEKLY")
     * @param category "TOTAL", "KNOWLEDGE", "REVIEW", "SINCERITY" (기본값 "TOTAL")
     * @param limit 가져올 랭커 수 (기본값 10, 최대 100 권장)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RankerResponseDto>>> getRanking(
            @RequestParam(defaultValue = "WEEKLY") String period,
            @RequestParam(defaultValue = "TOTAL") String category,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<RankerResponseDto> rankers = rankingService.getTopRankers(period, category, limit);
        return ApiResponse.success(rankers);
    }

    /**
     * 실시간 급상승 공고 랭킹 조회 (Hot Notices)
     */
    @GetMapping("/hot-notices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHotNotices(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<HotNoticeResponseDto> hotNotices = rankingService.getHotNotices(limit);
        String updatedAt = rankingService.getHotNoticeUpdatedAt();
        
        Map<String, Object> response = new HashMap<>();
        response.put("notices", hotNotices);
        response.put("updatedAt", updatedAt);
        
        return ApiResponse.success(response);
    }
}
