package com.habidue.app.controller;

import com.habidue.app.domain.usernotice.UserNotice;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.usernotice.UserNoticeRequestDto;
import com.habidue.app.dto.usernotice.UserNoticeResponseDto;
import com.habidue.app.dto.usernotice.UserNoticeUpdateDto; // 추가 예정
import com.habidue.app.service.notice.UserNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-notices")
@RequiredArgsConstructor
public class UserNoticeController {

    private final UserNoticeService userNoticeService;

    @PostMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserNoticeResponseDto>> addUserNotice(@Valid @RequestBody UserNoticeRequestDto userNoticeRequestDto) {
        return ApiResponse.success(HttpStatus.CREATED, userNoticeService.addUserNotice(userNoticeRequestDto));
    }

    @GetMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Page<UserNoticeResponseDto>>> getMyUserNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);
        
        return ApiResponse.success(userNoticeService.getMyUserNotices(pageable));
    }

    // 메모 및 참고 URL 통합 업데이트 엔드포인트
    @PutMapping("/{id}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<UserNoticeResponseDto>> updateUserInfo(
            @PathVariable Long id, 
            @RequestBody UserNoticeUpdateDto updateDto) {
        UserNoticeResponseDto updated = userNoticeService.updateUserInfo(
                id, 
                updateDto.getMemo(), 
                updateDto.getReferenceUrls(),
                updateDto.getUserDeadline());
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/{id}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> deleteUserNotice(@PathVariable Long id) {
        userNoticeService.deleteUserNotice(id);
        return ApiResponse.success(HttpStatus.NO_CONTENT, null);
    }

    @DeleteMapping("/notice/{noticeId}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> deleteUserNoticeByNoticeId(@PathVariable Long noticeId) {
        userNoticeService.deleteUserNoticeByNoticeId(noticeId);
        return ApiResponse.success(HttpStatus.NO_CONTENT, null);
    }
}
