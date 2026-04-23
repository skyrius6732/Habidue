package com.habidue.app.controller;

import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserStatus;
import com.habidue.app.domain.usernotice.UserNotice;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.user.UserResponseDto;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.notice.UserNoticeRepository;
import com.habidue.app.service.mail.ReportEmailService;
import com.habidue.app.service.mail.ReportService;
import com.habidue.app.service.user.UserService;
import com.habidue.app.service.user.UserEffectService;
import com.habidue.app.service.excel.ExcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserNoticeRepository userNoticeRepository;
    private final ReportEmailService reportEmailService;
    private final ReportService reportService;
    private final UserService userService;
    private final UserEffectService userEffectService;
    private final ExcelService excelService;
    private final com.habidue.app.service.user.KarmaService karmaService;
    private final com.habidue.app.service.board.PostService postService;
    private final com.habidue.app.service.board.CommentService commentService;
    private final com.habidue.app.service.ranking.RankingService rankingService;

    /**
     * [시니어 조치] 사용자 검색 (관리자용)
     */
    @GetMapping
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> searchUsers(
            @RequestParam String search,
            @RequestParam(defaultValue = "10") int limit) {

        if (search == null || search.trim().isEmpty()) {
            return ApiResponse.success(new java.util.ArrayList<>());
        }

        String searchTerm = search.toLowerCase();
        List<User> users = userRepository.findAll().stream()
            .filter(u -> u.getStatus() != UserStatus.WITHDRAWN)
            .filter(u -> u.getUsername().toLowerCase().contains(searchTerm) ||
                        (u.getNickname() != null && u.getNickname().toLowerCase().contains(searchTerm)) ||
                        u.getEmail().toLowerCase().contains(searchTerm))
            .limit(limit)
            .toList();

        List<UserResponseDto> result = users.stream()
            .map(u -> {
                UserResponseDto dto = new UserResponseDto(u);
                dto.setOwnedEffectCodes(userEffectService.getUserEffectCodes(u.getId()));
                return dto;
            })
            .toList();

        return ApiResponse.success(result);
    }

    /**
     * [시니어 조치] 전체 활성 사용자 목록 조회 (탈퇴 제외, 전체선택용)
     */
    @GetMapping("/admin/active-list")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllActiveUsers() {
        List<UserResponseDto> result = userRepository.findAll().stream()
            .filter(u -> u.getStatus() != UserStatus.WITHDRAWN)
            .map(u -> {
                UserResponseDto dto = new UserResponseDto(u);
                dto.setOwnedEffectCodes(userEffectService.getUserEffectCodes(u.getId()));
                return dto;
            })
            .toList();
        return ApiResponse.success(result);
    }

    @GetMapping("/me")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        UserResponseDto dto = new UserResponseDto(user);
        // [시니어 조치] 사용자가 소유한 이펙트 목록 추가
        dto.setOwnedEffectCodes(userEffectService.getUserEffectCodes(user.getId()));
        return ApiResponse.success(dto);
    }

    @PatchMapping("/me/nickname")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateNickname(@RequestParam String nickname) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        try {
            User updatedUser = userService.updateNickname(user.getId(), nickname);
            return ApiResponse.success(new UserResponseDto(updatedUser));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, e.getMessage(), "Duplicate Nickname");
        }
    }

    @PatchMapping("/me/email-report")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserResponseDto>> toggleEmailReport(@RequestParam boolean enabled) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        if (enabled && !user.isReportEmailVerified()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다.", "Email Not Verified");
        }
        
        user.setEmailReportEnabled(enabled);
        userRepository.save(user);
        return ApiResponse.success(new UserResponseDto(user));
    }

    @PostMapping("/me/report-email/send-code")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> sendCode(@RequestParam String email) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        reportEmailService.sendVerificationCode(user, email);
        return ApiResponse.success(null);
    }

    @PostMapping("/me/report-email/verify-code")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(@RequestParam String email, @RequestParam String code) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        boolean success = reportEmailService.verifyCode(user, email, code);
        return ApiResponse.success(success);
    }

    @PostMapping("/me/email-report/test")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<String>> testEmailReport() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        reportService.sendUserReport(user);
        return ApiResponse.success("테스트 리포트 발송 요청 완료. 메일함을 확인해 주세요.");
    }

    /**
     * [시니어 조치] 유저 특수 효과(날개 등) 장착/변경
     */
    @PatchMapping("/{publicId}/effect")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public ResponseEntity<ApiResponse<UserResponseDto>> updateEffect(
            @PathVariable String publicId,
            @RequestParam(required = false) String effectCode) {
        
        User targetUser = userRepository.findByPublicId(publicId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        // 본인 또는 관리자만 수정 가능
        if (!currentUser.getId().equals(targetUser.getId()) && !currentUser.getRole().name().equals("ROLE_ADMIN")) {
            return ApiResponse.error(HttpStatus.FORBIDDEN, "권한이 없습니다.", "Forbidden");
        }
        
        User updatedUser = userService.updateEquippedEffect(targetUser.getId(), effectCode);
        return ApiResponse.success(new UserResponseDto(updatedUser));
    }

    /**
     * [시니어 조치] 단일 사용자를 베타테스터로 설정 (관리자만)
     */
    @PatchMapping("/admin/{userId}/beta-tester")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<UserResponseDto>> setBetaTester(
            @PathVariable Long userId,
            @RequestParam boolean enabled) {
        
        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        userEffectService.setBetaTester(targetUser.getId(), enabled);
        return ApiResponse.success(new UserResponseDto(targetUser));
    }

    /**
     * [시니어 조치] 여러 사용자를 일괄 베타테스터로 설정 (관리자만)
     */
    @PatchMapping("/admin/batch/beta-tester")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> setBetaTesterBatch(
            @RequestParam List<Long> userIds,
            @RequestParam boolean enabled) {

        if (userIds == null || userIds.isEmpty()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, "사용자 ID 리스트가 필요합니다.", "Empty List");
        }

        int successCount = 0;
        for (Long userId : userIds) {
            try {
                userEffectService.setBetaTester(userId, enabled);
                successCount++;
            } catch (Exception e) {
                log.warn("베타테스터 설정 실패. userId={}, error={}", userId, e.getMessage());
            }
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalRequested", userIds.size());
        result.put("successCount", successCount);
        result.put("status", enabled ? "베타테스터 추가" : "베타테스터 제거");

        return ApiResponse.success(result);
    }

    /**
     * [시니어 조치] 단일 사용자에게 이펙트 지급 (관리자만) - publicId 기반
     */
    @PostMapping("/admin/{publicId}/grant-effect")
    @Secured("ROLE_ADMIN")
    @Transactional
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> grantEffect(
            @PathVariable String publicId,
            @RequestParam String effectCode,
            @RequestParam(defaultValue = "EVENT") String source) {

        User targetUser = userRepository.findByPublicId(publicId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        try {
            com.habidue.app.domain.user.UserEffect.EffectSource effectSource =
                com.habidue.app.domain.user.UserEffect.EffectSource.valueOf(source.toUpperCase());

            userEffectService.grantEffect(targetUser.getId(), effectCode, effectSource);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("publicId", publicId);
            result.put("effectCode", effectCode);
            result.put("source", source);
            result.put("message", "이펙트 지급 완료");

            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, e.getMessage(), "Invalid Source");
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "이펙트 지급 실패: " + e.getMessage(), "Grant Failed");
        }
    }

    /**
     * [시니어 조치] 여러 사용자에게 이펙트 일괄 지급 (관리자만) - publicId + 복수 effectCodes 기반
     */
    @PostMapping("/admin/batch/grant-effect")
    @Secured("ROLE_ADMIN")
    @Transactional
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> grantEffectBatch(
            @RequestParam List<String> publicIds,
            @RequestParam List<String> effectCodes,
            @RequestParam(defaultValue = "EVENT") String source) {

        if (publicIds == null || publicIds.isEmpty()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, "사용자 ID 리스트가 필요합니다.", "Empty List");
        }
        if (effectCodes == null || effectCodes.isEmpty()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, "이펙트 코드가 필요합니다.", "Empty Effects");
        }

        try {
            com.habidue.app.domain.user.UserEffect.EffectSource effectSource =
                com.habidue.app.domain.user.UserEffect.EffectSource.valueOf(source.toUpperCase());

            int successCount = 0;
            int alreadyOwned = 0;

            for (String publicId : publicIds) {
                User user = userRepository.findByPublicId(publicId).orElse(null);
                if (user == null) continue;
                for (String effectCode : effectCodes) {
                    try {
                        if (userEffectService.hasEffect(user.getId(), effectCode)) {
                            alreadyOwned++;
                        } else {
                            userEffectService.grantEffect(user.getId(), effectCode, effectSource);
                            successCount++;
                        }
                    } catch (Exception e) {
                        log.warn("이펙트 지급 실패. publicId={}, effectCode={}, error={}", publicId, effectCode, e.getMessage());
                    }
                }
            }

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("totalRequested", publicIds.size());
            result.put("effectCount", effectCodes.size());
            result.put("successCount", successCount);
            result.put("alreadyOwned", alreadyOwned);
            result.put("source", source);
            result.put("message", String.format("신규 지급: %d건, 이미 소유: %d건", successCount, alreadyOwned));

            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, "유효하지 않은 source: " + e.getMessage(), "Invalid Source");
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "일괄 지급 실패: " + e.getMessage(), "Batch Grant Failed");
        }
    }

    /**
     * [시니어 조치] 단일 사용자 이펙트 회수 (관리자만) - publicId 기반
     */
    @DeleteMapping("/admin/{publicId}/revoke-effect")
    @Secured("ROLE_ADMIN")
    @Transactional
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> revokeEffect(
            @PathVariable String publicId,
            @RequestParam String effectCode) {

        User targetUser = userRepository.findByPublicId(publicId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        userEffectService.revokeEffect(targetUser.getId(), effectCode);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("publicId", publicId);
        result.put("effectCode", effectCode);
        result.put("message", "이펙트 회수 완료");

        return ApiResponse.success(result);
    }

    /**
     * [시니어 조치] 여러 사용자 이펙트 일괄 회수 (관리자만) - publicId 기반
     */
    @DeleteMapping("/admin/batch/revoke-effect")
    @Secured("ROLE_ADMIN")
    @Transactional
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> revokeEffectBatch(
            @RequestParam List<String> publicIds,
            @RequestParam List<String> effectCodes) {

        if (publicIds == null || publicIds.isEmpty()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, "사용자 ID 리스트가 필요합니다.", "Empty List");
        }
        if (effectCodes == null || effectCodes.isEmpty()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, "이펙트 코드가 필요합니다.", "Empty Effects");
        }

        int successCount = 0;
        int notOwned = 0;

        for (String publicId : publicIds) {
            User user = userRepository.findByPublicId(publicId).orElse(null);
            if (user == null) continue;
            for (String effectCode : effectCodes) {
                try {
                    if (!userEffectService.hasEffect(user.getId(), effectCode)) {
                        notOwned++;
                    } else {
                        userEffectService.revokeEffect(user.getId(), effectCode);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.warn("이펙트 회수 실패. publicId={}, effectCode={}, error={}", publicId, effectCode, e.getMessage());
                }
            }
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalRequested", publicIds.size());
        result.put("effectCount", effectCodes.size());
        result.put("successCount", successCount);
        result.put("notOwned", notOwned);
        result.put("message", String.format("회수 완료: %d건, 미소유: %d건", successCount, notOwned));

        return ApiResponse.success(result);
    }

    @DeleteMapping("/me")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        userService.deleteUser(user.getId());
        return ApiResponse.success(null);
    }

    @GetMapping("/me/export")
    @Secured("ROLE_USER")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportMyNotices() throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        List<UserNotice> userNotices = userNoticeRepository.findAllByUserWithNotice(user);
        byte[] excelData = excelService.exportUserNoticesToExcel(userNotices);
        
        String displayName = user.getNickname() != null ? user.getNickname() : user.getUsername();
        String fileName = URLEncoder.encode("HabiDue_관심공고_" + displayName + ".xlsx", StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

    /**
     * [시니어 조치] 내 활동 통계 및 배지 정보 조회
     */
    @GetMapping("/me/activity")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<com.habidue.app.dto.user.UserActivityResponseDto>> getMyActivity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ApiResponse.success(userService.getUserActivity(user.getId()));
    }

    /**
     * [시니어 조치] 내 배지 정보 수동 동기화 (테스트 및 데이터 정정용)
     */
    @PostMapping("/me/activity/sync")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> syncMyActivity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        userService.syncUserActivity(user.getId());
        
        return ApiResponse.success(null);
    }

    /**
     * [시니어 조치] 대표 배지(칭호) 장착/변경
     */
    @PatchMapping("/me/equipped-badge")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateEquippedBadge(@RequestParam(required = false) Long badgeId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        User updatedUser = userService.updateEquippedBadge(user.getId(), badgeId);
        return ApiResponse.success(new UserResponseDto(updatedUser));
    }

    /**
     * [시니어 조치] 레벨링 시각 효과 활성화 여부 토글
     */
    @PatchMapping("/me/level-effects")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserResponseDto>> toggleLevelEffects(@RequestParam boolean enabled) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        user.setShowLevelEffects(enabled);
        userRepository.save(user);
        return ApiResponse.success(new UserResponseDto(user));
    }

    /**
     * [시니어 조치] 이펙트 시각 효과 활성화 여부 토글
     */
    @PatchMapping("/me/show-equipped-effect")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserResponseDto>> toggleEquippedEffect(@RequestParam boolean enabled) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        user.setShowEquippedEffect(enabled);
        userRepository.save(user);
        return ApiResponse.success(new UserResponseDto(user));
    }

    /**
     * [시니어 조치] 장착 중인 닉네임 스타일 티어 변경
     */
    @PatchMapping("/me/nickname-tier")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateEquippedTier(@RequestParam(required = false) Integer tier) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        // 검증: 본인 레벨보다 높은 티어는 장착 불가 (관리자 제외)
        if (tier != null && tier > user.getLevel() && !user.getRole().name().equals("ROLE_ADMIN")) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST, "해당 레벨 달성 후 장착 가능합니다.", "Invalid Tier");
        }

        user.setEquippedTier(tier);
        userRepository.save(user);
        return ApiResponse.success(new UserResponseDto(user));
    }

    /**
     * [시니어 조치] 내 활동 신뢰 점수(Karma) 변동 이력 조회
     */
    @GetMapping("/me/karma-history")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<List<com.habidue.app.dto.user.KarmaHistoryResponseDto>>> getMyKarmaHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ApiResponse.success(karmaService.getKarmaHistory(user.getId()));
    }

    /**
     * [시니어 조치] 내 게시글 목록 조회 (페이징)
     */
    @GetMapping("/me/posts")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<com.habidue.app.dto.board.PostResponseDto>>> getMyPosts(org.springframework.data.domain.Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ApiResponse.success(postService.getMyPosts(user.getId(), pageable));
    }

    /**
     * [시니어 조치] 내 댓글 목록 조회 (페이징)
     */
    @GetMapping("/me/comments")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<com.habidue.app.dto.board.CommentResponseDto>>> getMyComments(org.springframework.data.domain.Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ApiResponse.success(commentService.getMyComments(user.getId(), pageable));
    }
}
