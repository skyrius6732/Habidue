package com.habidue.app.controller.admin;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.user.Role;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.admin.AdminUserResponseDto;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.service.user.KarmaService;
import com.habidue.app.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class UserAdminController {

    private final UserService userService;
    private final KarmaService karmaService;
    private final UserRepository userRepository;

    /**
     * 전체 사용자 목록 조회 (관리자 전용)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserResponseDto>>> getAllUsers() {
        List<AdminUserResponseDto> users = userService.getAllUsers().stream()
                .map(user -> AdminUserResponseDto.from(user, userService.isUserOnline(user.getId())))
                .collect(Collectors.toList());
        return ApiResponse.success(users);
    }

    /**
     * 사용자 역할(Role) 변경 (관리자 전용)
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<AdminUserResponseDto>> updateUserRole(
            @PathVariable Long userId,
            @RequestParam com.habidue.app.domain.user.Role role) {
        User updatedUser = userService.updateUserRole(userId, role);
        return ApiResponse.success(AdminUserResponseDto.from(updatedUser, userService.isUserOnline(userId)));
    }

    /**
     * 사용자 상태(Status) 변경 (차단/해제 등)
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponseDto>> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam com.habidue.app.domain.user.UserStatus status,
            @RequestParam(required = false) String reason) {
        User updatedUser = userService.updateUserStatus(userId, status, reason);
        return ApiResponse.success(AdminUserResponseDto.from(updatedUser, userService.isUserOnline(userId)));
    }

    /**
     * [시니어 조치] 사용자 카르마 점수 수동 조정
     */
    @PatchMapping("/{userId}/karma")
    public ResponseEntity<ApiResponse<AdminUserResponseDto>> updateUserKarma(
            @PathVariable Long userId,
            @RequestParam double points,
            @RequestParam(required = false) com.habidue.app.domain.user.KarmaReason reason,
            @RequestParam String reasonText, // 상세 사유(comment)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        User admin = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NoSuchElementException("관리자 정보를 찾을 수 없습니다."));
        
        // [시니어] 0.1P 단위 대응을 위해 manualAdjustKarmaRaw 호출 (points * 10)
        int rawDelta = (int) Math.round(points * com.habidue.app.service.user.KarmaService.KARMA_SCALE);
        karmaService.manualAdjustKarmaRaw(userId, rawDelta, reason, reasonText, admin, true);
        
        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        return ApiResponse.success(AdminUserResponseDto.from(updatedUser, userService.isUserOnline(userId)));
    }

    /**
     * [시니어 조치] 사용 가능한 카르마 사유 목록 조회
     */
    @GetMapping("/karma-reasons")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, String>>>> getKarmaReasons() {
        java.util.List<java.util.Map<String, String>> reasons = java.util.Arrays.stream(com.habidue.app.domain.user.KarmaReason.values())
                .map(r -> {
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("value", r.name());
                    map.put("label", r.getDescription());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(reasons);
    }

    /**
     * [시니어 조치] 특정 사용자의 카르마 히스토리 조회
     */
    @GetMapping("/{userId}/karma-history")
    public ResponseEntity<ApiResponse<java.util.List<com.habidue.app.dto.user.KarmaHistoryResponseDto>>> getKarmaHistory(
            @PathVariable Long userId) {
        return ApiResponse.success(karmaService.getKarmaHistory(userId));
    }

    /**
     * [시니어 조치] 사용자 활동 제한 즉시 해제
     */
    @DeleteMapping("/{userId}/restriction")
    public ResponseEntity<ApiResponse<AdminUserResponseDto>> liftUserRestriction(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        User admin = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NoSuchElementException("관리자 정보를 찾을 수 없습니다."));
        
        karmaService.liftRestriction(userId, admin);
        
        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        return ApiResponse.success(AdminUserResponseDto.from(updatedUser, userService.isUserOnline(userId)));
    }
}
