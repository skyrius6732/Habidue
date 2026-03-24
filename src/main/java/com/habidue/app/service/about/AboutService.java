package com.habidue.app.service.about;

import com.habidue.app.domain.about.Announcement;
import com.habidue.app.domain.about.PatchNote;
import com.habidue.app.domain.about.PatchNoteDetail;
import com.habidue.app.domain.notification.NotificationType;
import com.habidue.app.dto.about.*;
import com.habidue.app.repository.about.AnnouncementRepository;
import com.habidue.app.repository.about.PatchNoteRepository;
import com.habidue.app.service.notification.NotificationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AboutService {

    private final AnnouncementRepository announcementRepository;
    private final PatchNoteRepository patchNoteRepository;
    private final EntityManager entityManager;
    private final NotificationService notificationService;

    // --- 공지사항 (Announcement) ---
    public List<AnnouncementResponseDto> getAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AnnouncementResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnnouncementResponseDto createAnnouncement(AnnouncementRequestDto dto) {
        Announcement announcement = announcementRepository.save(Announcement.builder()
                .tag(dto.getTag()).type(dto.getType()).date(dto.getDate())
                .title(dto.getTitle()).content(dto.getContent()).build());
        
        // [시니어 조치] 전체 알림 발송 옵션 처리
        if (dto.isSendNotification()) {
            String content = String.format("%s [공지] %s", dto.getTag(), dto.getTitle());
            notificationService.sendToAllUsers(
                NotificationType.SYSTEM,
                content,
                null, // relatedTargetId (상세 페이지 링크가 필요하다면 추가 설계 필요)
                null  // postId
            );
        }
        
        return new AnnouncementResponseDto(announcement);
    }

    @Transactional
    public AnnouncementResponseDto updateAnnouncement(Long id, AnnouncementRequestDto dto) {
        Announcement announcement = announcementRepository.findById(id).orElseThrow();
        announcement.update(dto.getTag(), dto.getType(), dto.getDate(), dto.getTitle(), dto.getContent());
        return new AnnouncementResponseDto(announcement);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        announcementRepository.deleteById(id);
    }

    // --- 패치 노트 (PatchNote) ---
    public List<PatchNoteResponseDto> getPatchNotes() {
        return patchNoteRepository.findAllWithDetailsOrderByCreatedAtDesc().stream()
                .map(PatchNoteResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public PatchNoteResponseDto createPatchNote(PatchNoteRequestDto dto) {
        PatchNote patchNote = PatchNote.builder()
                .title(dto.getTitle())
                .version(dto.getVersion())
                .date(dto.getDate())
                .build();
        
        List<String> detailContents = dto.getDetails() != null ? dto.getDetails() : List.of();
        List<PatchNoteDetail> details = detailContents.stream()
                .map(content -> PatchNoteDetail.builder().patchNote(patchNote).content(content).build())
                .collect(Collectors.toList());
        
        patchNote.getDetails().addAll(details);
        return new PatchNoteResponseDto(patchNoteRepository.save(patchNote));
    }

    @Transactional
    public PatchNoteResponseDto updatePatchNote(Long id, PatchNoteRequestDto dto) {
        PatchNote patchNote = patchNoteRepository.findById(id).orElseThrow();
        patchNote.update(dto.getTitle(), dto.getVersion(), dto.getDate());
        
        // 기존 상세 내역 삭제 및 즉시 반영 (순서 보장 핵심)
        patchNote.getDetails().clear();
        entityManager.flush();
        
        List<String> detailContents = dto.getDetails() != null ? dto.getDetails() : List.of();
        List<PatchNoteDetail> details = detailContents.stream()
                .map(content -> PatchNoteDetail.builder().patchNote(patchNote).content(content).build())
                .collect(Collectors.toList());
        patchNote.getDetails().addAll(details);
        
        return new PatchNoteResponseDto(patchNote);
    }

    @Transactional
    public void deletePatchNote(Long id) {
        patchNoteRepository.deleteById(id);
    }
}
