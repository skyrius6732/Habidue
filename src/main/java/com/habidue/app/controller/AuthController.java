package com.habidue.app.controller;

import com.habidue.app.config.jwt.JwtTokenProvider;
import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.SignupRequest;
import com.habidue.app.service.user.AuthService;
import com.habidue.app.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/test/user") // 이 경로는 /api/auth/test/user 가 됨
    public ResponseEntity<String> getAuthenticatedUser(@org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        return ResponseEntity.ok("Authenticated user email: " + userPrincipal.getEmail() + ", ID: " + userPrincipal.getId());
    }

    @PostMapping("/signup") // 이 경로는 /api/auth/signup 이 됨
    public ResponseEntity<ApiResponse<String>> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        userService.registerUser(signupRequest);
        return ApiResponse.success(HttpStatus.CREATED, "User registered successfully!");
    }

    @PostMapping("/reissue") // 이 경로는 /api/auth/reissue 가 됨
    public ResponseEntity<ApiResponse<String>> reissue(@RequestHeader("Authorization-Refresh") String refreshToken) {
        String newAccessToken = authService.reissueAccessToken(refreshToken);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + newAccessToken)
                .body(ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message("Access token reissued")
                        .data(null)
                        .build());
    }

    /**
     * 로그아웃 처리 (실시간 접속 상태 제거)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal != null) {
            userService.setUserOffline(userPrincipal.getId());
        }
        return ApiResponse.success(null);
    }
}
