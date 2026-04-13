package com.habidue.app.controller.admin;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.inquiry.InquiryStatus;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.admin.AdminInquiryResponseDto;
import com.habidue.app.dto.inquiry.InquiryAnswerRequestDto;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.service.inquiry.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminInquiryController {

    private final InquiryService inquiryService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminInquiryResponseDto>>> getAllInquiries(@RequestParam(required = false) InquiryStatus status,
                                                                                 @RequestParam(required = false) String keyword,
                                                                                 Pageable pageable) {
        return ApiResponse.success(inquiryService.getAllInquiries(status, keyword, pageable));
    }

    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<ApiResponse<Void>> answerInquiry(@PathVariable Long inquiryId,
                                                           @RequestBody InquiryAnswerRequestDto answerDto,
                                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User admin = userRepository.findById(userPrincipal.getId()).orElseThrow();
        inquiryService.answerInquiry(inquiryId, answerDto, admin);
        return ApiResponse.success(null);
    }
}
