package com.habidue.app.controller;

import com.habidue.app.domain.user.User;
import com.habidue.app.domain.usernotice.UserNotice;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.user.UserResponseDto;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.notice.UserNoticeRepository;
import com.habidue.app.service.mail.ReportEmailService;
import com.habidue.app.service.mail.ReportService;
import com.habidue.app.service.user.UserService;
import com.habidue.app.service.excel.ExcelService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserNoticeRepository userNoticeRepository;
    private final ReportEmailService reportEmailService;
    private final ReportService reportService;
    private final UserService userService;
    private final ExcelService excelService;
    private final com.habidue.app.service.user.KarmaService karmaService;
    private final com.habidue.app.service.board.PostService postService;
    private final com.habidue.app.service.board.CommentService commentService;

    @GetMapping("/me")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return ApiResponse.success(new UserResponseDto(user));
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
    @PatchMapping("/{id}/effect")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public ResponseEntity<ApiResponse<UserResponseDto>> updateEffect(
            @PathVariable Long id,
            @RequestParam(required = false) String effectCode) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        // 본인 또는 관리자만 수정 가능
        if (!currentUser.getId().equals(id) && !currentUser.getRole().name().equals("ROLE_ADMIN")) {
            return ApiResponse.error(HttpStatus.FORBIDDEN, "권한이 없습니다.", "Forbidden");
        }
        
        User updatedUser = userService.updateEquippedEffect(id, effectCode);
        return ApiResponse.success(new UserResponseDto(updatedUser));
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
