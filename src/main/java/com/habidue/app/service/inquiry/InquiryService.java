package com.habidue.app.service.inquiry;

import com.habidue.app.domain.inquiry.Inquiry;
import com.habidue.app.domain.inquiry.InquiryStatus;
import com.habidue.app.domain.notification.NotificationType;
import com.habidue.app.domain.user.Role;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.inquiry.InquiryAnswerRequestDto;
import com.habidue.app.dto.inquiry.InquiryRequestDto;
import com.habidue.app.dto.inquiry.InquiryResponseDto;
import com.habidue.app.repository.inquiry.InquiryRepository;
import com.habidue.app.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final NotificationService notificationService;

    @Value("${file.upload-dir:/home/skyrius/habiDue/uploads/inquiries}")
    private String uploadDir;

    @Transactional
    public Long createInquiry(InquiryRequestDto requestDto, org.springframework.web.multipart.MultipartFile image, User author) {
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = saveImage(image);
        }

        Inquiry inquiry = Inquiry.builder()
                .author(author)
                .category(requestDto.getCategory())
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .imageUrl(imageUrl)
                .build();
        return inquiryRepository.save(inquiry).getId();
    }

    private String saveImage(org.springframework.web.multipart.MultipartFile file) {
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedName = java.util.UUID.randomUUID().toString() + extension;
        
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(uploadDir, savedName);
            java.nio.file.Files.copy(file.getInputStream(), path);
            return "/uploads/inquiries/" + savedName;
        } catch (java.io.IOException e) {
            throw new RuntimeException("이미지 저장 실패", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponseDto> getMyInquiries(User author, Pageable pageable) {
        return inquiryRepository.findByAuthorAndDeletedByAuthorFalseOrderByCreatedAtDesc(author, pageable)
                .map(InquiryResponseDto::new);
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponseDto> getAllInquiries(InquiryStatus status, String keyword, Pageable pageable) {
        return inquiryRepository.findInquiriesWithSearch(status, keyword, pageable)
                .map(InquiryResponseDto::new);
    }

    @Transactional(readOnly = true)
    public InquiryResponseDto getInquiry(Long inquiryId, User user) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("문의 내역을 찾을 수 없습니다."));
        
        // 작성자 본인 또는 관리자만 확인 가능 (비밀 상담 원칙)
        boolean isAdmin = user.getRole() == Role.ADMIN;
        
        if (!inquiry.getAuthor().getId().equals(user.getId()) && !isAdmin) {
            throw new IllegalArgumentException("조회 권한이 없습니다.");
        }

        // 사용자가 삭제한 건은 관리자만 조회 가능
        if (inquiry.isDeletedByAuthor() && !isAdmin) {
            throw new IllegalArgumentException("삭제된 문의 내역입니다.");
        }
        
        return new InquiryResponseDto(inquiry);
    }

    @Transactional
    public void answerInquiry(Long inquiryId, InquiryAnswerRequestDto answerDto, User admin) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("문의 내역을 찾을 수 없습니다."));
        
        inquiry.answer(answerDto.getAnswer(), admin);
        
        // [시니어 조치] 작성자에게 실시간 알림 발송 시 문의 ID(relatedTargetId)를 포함하여 자동 포커싱 지원
        notificationService.send(inquiry.getAuthor(), 
                NotificationType.SYSTEM, 
                "📢 문의하신 내용에 대한 답변이 등록되었습니다: \"" + inquiry.getTitle() + "\"", 
                inquiry.getId(), 
                null);
    }

    @Transactional
    public void deleteInquiry(Long inquiryId, User user) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("문의 내역을 찾을 수 없습니다."));
        
        boolean isAdmin = user.getRole() == Role.ADMIN;
        
        if (!inquiry.getAuthor().getId().equals(user.getId()) && !isAdmin) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        
        // 물리 삭제 대신 논리 삭제 처리 (사용자에게만 안 보임)
        inquiry.deleteByAuthor();
    }
}
