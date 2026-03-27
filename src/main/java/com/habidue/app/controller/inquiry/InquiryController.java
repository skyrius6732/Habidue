package com.habidue.app.controller.inquiry;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.inquiry.InquiryRequestDto;
import com.habidue.app.dto.inquiry.InquiryResponseDto;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.service.inquiry.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final UserRepository userRepository;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Long>> createInquiry(@RequestPart("inquiry") InquiryRequestDto requestDto,
                                                          @RequestPart(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow();
        return ApiResponse.success(inquiryService.createInquiry(requestDto, image, user));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<InquiryResponseDto>>> getMyInquiries(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                                Pageable pageable) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow();
        return ApiResponse.success(inquiryService.getMyInquiries(user, pageable));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryResponseDto>> getInquiry(@PathVariable Long inquiryId,
                                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow();
        return ApiResponse.success(inquiryService.getInquiry(inquiryId, user));
    }

    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<Void>> deleteInquiry(@PathVariable Long inquiryId,
                                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow();
        inquiryService.deleteInquiry(inquiryId, user);
        return ApiResponse.success(null);
    }
}
