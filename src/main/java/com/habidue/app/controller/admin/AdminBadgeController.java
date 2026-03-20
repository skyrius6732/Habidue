package com.habidue.app.controller.admin;

import com.habidue.app.domain.badge.BadgeLevelRule;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.admin.BadgeLevelRuleRequestDto;
import com.habidue.app.service.badge.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자용 배지 운영 API
 */
@RestController
@RequestMapping("/api/admin/badges")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN") // 관리자 권한 필수
public class AdminBadgeController {

    private final BadgeService badgeService;

    /**
     * 모든 배지 레벨 규칙 조회
     */
    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<BadgeLevelRule>>> getAllRules() {
        return ApiResponse.success(badgeService.getAllRules());
    }

    /**
     * 특정 배지 규칙 수정
     */
    @PutMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<Void>> updateRule(
            @PathVariable Long id,
            @RequestBody BadgeLevelRuleRequestDto requestDto) {
        badgeService.updateRule(id, requestDto);
        return ApiResponse.success(null);
    }

    /**
     * 모든 베이스 배지(마스터) 조회
     */
    @GetMapping("/master")
    public ResponseEntity<ApiResponse<List<com.habidue.app.domain.badge.Badge>>> getAllMasters() {
        return ApiResponse.success(badgeService.getAllMasters());
    }

    /**
     * 새 베이스 배지(마스터) 생성
     */
    @PostMapping("/master")
    public ResponseEntity<ApiResponse<Void>> createMaster(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String type,
            @RequestParam(required = false) String badgeTip) {
        badgeService.createBadgeMaster(code, name, description, type, badgeTip);
        return ApiResponse.success(null);
    }

    /**
     * 새 배지 레벨 규칙 생성
     */
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<Void>> createRule(
            @RequestParam String type,
            @RequestParam int level,
            @RequestBody BadgeLevelRuleRequestDto requestDto) {
        badgeService.createRule(type, requestDto, level);
        return ApiResponse.success(null);
    }

    /**
     * 배지 규칙 삭제
     */
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        badgeService.deleteRule(id);
        return ApiResponse.success(null);
    }

    /**
     * 배지 마스터 삭제 (관련 규칙 포함)
     */
    @DeleteMapping("/master/{code}")
    public ResponseEntity<ApiResponse<Void>> deleteMaster(@PathVariable String code) {
        badgeService.deleteBadgeMaster(code);
        return ApiResponse.success(null);
    }

    /**
     * 전체 유저 배지 정보 일괄 재동기화
     */
    @PostMapping("/sync-all")
    public ResponseEntity<ApiResponse<String>> syncAll() {
        badgeService.syncAllUsersBadges();
        return ApiResponse.success("전체 유저 배지 정보가 성공적으로 재동기화되었습니다.");
    }
}
