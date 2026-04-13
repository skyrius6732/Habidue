package com.habidue.app.service.inquiry;

import com.habidue.app.domain.inquiry.Inquiry;
import com.habidue.app.domain.inquiry.InquiryStatus;
import com.habidue.app.domain.notification.NotificationType;
import com.habidue.app.domain.user.Role;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.admin.AdminInquiryResponseDto;
import com.habidue.app.dto.inquiry.InquiryAnswerRequestDto;
import com.habidue.app.dto.inquiry.InquiryRequestDto;
import com.habidue.app.dto.inquiry.InquiryResponseDto;
import com.habidue.app.repository.inquiry.InquiryRepository;
import com.habidue.app.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
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
    private final com.habidue.app.service.storage.FileStorageService fileStorageService;

    @Transactional
    public Long createInquiry(InquiryRequestDto requestDto, org.springframework.web.multipart.MultipartFile image, User author) {
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                java.util.List<String> uploadedUrls = fileStorageService.upload(java.util.List.of(image), "inquiries");
                if (!uploadedUrls.isEmpty()) {
                    imageUrl = uploadedUrls.get(0);
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
            }
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

    @Transactional(readOnly = true)
    public Page<InquiryResponseDto> getMyInquiries(User author, Pageable pageable) {
        return inquiryRepository.findByAuthorAndDeletedByAuthorFalseOrderByCreatedAtDesc(author, pageable)
                .map(InquiryResponseDto::new);
    }

    @Transactional(readOnly = true)
    public Page<AdminInquiryResponseDto> getAllInquiries(InquiryStatus status, String keyword, Pageable pageable) {
        return inquiryRepository.findInquiriesWithSearch(status, keyword, pageable)
                .map(AdminInquiryResponseDto::from);
    }

    @Transactional(readOnly = true)
    public InquiryResponseDto getInquiry(Long inquiryId, User user) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("문의 내역을 찾을 수 없습니다."));
        
        boolean isAdmin = user.getRole() == Role.ADMIN;
        
        if (!inquiry.getAuthor().getId().equals(user.getId()) && !isAdmin) {
            throw new IllegalArgumentException("조회 권한이 없습니다.");
        }

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
        
        inquiry.deleteByAuthor();
    }
}
