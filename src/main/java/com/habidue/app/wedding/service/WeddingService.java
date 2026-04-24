package com.habidue.app.wedding.service;

import com.habidue.app.wedding.domain.*;
import com.habidue.app.wedding.dto.*;
import com.habidue.app.wedding.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.habidue.app.service.storage.FileStorageService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WeddingService {

    private final WeddingInvitationRepository invitationRepository;
    private final WeddingPhotoRepository photoRepository;
    private final WeddingAccountRepository accountRepository;
    private final WeddingRsvpRepository rsvpRepository;
    private final WeddingGuestbookRepository guestbookRepository;
    private final FileStorageService fileStorageService;

    private static final int MAX_IMAGE_WIDTH = 1920;

    // ===================== 어드민: 청첩장 CRUD =====================

    public WeddingInvitationResponseDto create(WeddingInvitationRequestDto dto, List<MultipartFile> files) throws IOException {
        if (invitationRepository.existsBySlug(dto.getSlug())) {
            throw new IllegalArgumentException("이미 사용 중인 slug입니다: " + dto.getSlug());
        }

        WeddingInvitation invitation = WeddingInvitation.builder()
                .slug(dto.getSlug())
                .groomName(dto.getGroomName())
                .brideName(dto.getBrideName())
                .groomFatherName(dto.getGroomFatherName())
                .groomMotherName(dto.getGroomMotherName())
                .brideFatherName(dto.getBrideFatherName())
                .brideMotherName(dto.getBrideMotherName())
                .weddingDate(dto.getWeddingDate())
                .venueName(dto.getVenueName())
                .venueAddress(dto.getVenueAddress())
                .venueDetailAddress(dto.getVenueDetailAddress())
                .venueLat(dto.getVenueLat())
                .venueLng(dto.getVenueLng())
                .greetingMessage(dto.getGreetingMessage())
                .transportInfo(dto.getTransportInfo())
                .parkingInfo(dto.getParkingInfo())
                .musicUrl(dto.getMusicUrl())
                .musicAutoPlay(dto.isMusicAutoPlay())
                .template(dto.getTemplate() != null ? dto.getTemplate() : "default")
                .status(dto.getStatus() != null ? dto.getStatus() : InvitationStatus.DRAFT)
                .expiresAt(dto.getExpiresAt())
                .build();

        WeddingInvitation saved = invitationRepository.save(invitation);
        saveAccounts(saved, dto.getAccounts());

        if (files != null && !files.isEmpty()) {
            uploadPhotos(saved.getId(), files);
        }

        return new WeddingInvitationResponseDto(invitationRepository.findById(saved.getId()).orElseThrow());
    }

    public WeddingInvitationResponseDto update(Long id, WeddingInvitationRequestDto dto, List<MultipartFile> files) throws IOException {
        WeddingInvitation invitation = getInvitationById(id);

        if (!invitation.getSlug().equals(dto.getSlug()) && invitationRepository.existsBySlug(dto.getSlug())) {
            throw new IllegalArgumentException("이미 사용 중인 slug입니다: " + dto.getSlug());
        }

        invitation.update(
                dto.getSlug(), dto.getGroomName(), dto.getBrideName(),
                dto.getGroomFatherName(), dto.getGroomMotherName(),
                dto.getBrideFatherName(), dto.getBrideMotherName(),
                dto.getWeddingDate(), dto.getVenueName(), dto.getVenueAddress(),
                dto.getVenueDetailAddress(), dto.getVenueLat(), dto.getVenueLng(),
                dto.getGreetingMessage(), dto.getTransportInfo(), dto.getParkingInfo(),
                dto.getMusicUrl(), dto.isMusicAutoPlay(),
                dto.getTemplate() != null ? dto.getTemplate() : "default",
                dto.getStatus() != null ? dto.getStatus() : InvitationStatus.DRAFT,
                dto.getExpiresAt()
        );

        accountRepository.deleteByInvitationId(id);
        invitation.getAccounts().clear();
        saveAccounts(invitation, dto.getAccounts());

        if (dto.getDeletedPhotoIds() != null && !dto.getDeletedPhotoIds().isEmpty()) {
            for (Long photoId : dto.getDeletedPhotoIds()) {
                photoRepository.findById(photoId).ifPresent(photo -> {
                    if (photo.getInvitation().getId().equals(id)) {
                        invitation.getPhotos().remove(photo);
                        photoRepository.delete(photo);
                    }
                });
            }
        }

        if (files != null && !files.isEmpty()) {
            uploadPhotos(id, files);
        }

        return new WeddingInvitationResponseDto(invitationRepository.findById(id).orElseThrow());
    }

    public void delete(Long id) {
        WeddingInvitation invitation = getInvitationById(id);
        invitationRepository.delete(invitation);
    }

    @Transactional(readOnly = true)
    public List<WeddingInvitationResponseDto> getAll() {
        return invitationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(WeddingInvitationResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WeddingInvitationResponseDto getById(Long id) {
        return new WeddingInvitationResponseDto(getInvitationById(id));
    }

    // ===================== 어드민: 파일 처리 =====================

    public String uploadMusic(Long invitationId, MultipartFile file) throws IOException {
        getInvitationById(invitationId);
        List<String> urls = fileStorageService.upload(List.of(file), "wedding/music");
        return urls.get(0);
    }

    public List<String> uploadPhotos(Long invitationId, List<MultipartFile> files) throws IOException {
        WeddingInvitation invitation = getInvitationById(invitationId);
        List<MultipartFile> processedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            processedFiles.add(resizeImageIfNeeded(file));
        }

        List<String> urls = fileStorageService.upload(processedFiles, "wedding/" + invitation.getSlug());

        int currentMax = invitation.getPhotos().stream()
                .mapToInt(WeddingPhoto::getOrderNum)
                .max().orElse(-1);

        for (int i = 0; i < urls.size(); i++) {
            WeddingPhoto photo = WeddingPhoto.builder()
                    .invitation(invitation)
                    .imageUrl(urls.get(i))
                    .orderNum(currentMax + 1 + i)
                    .build();
            invitation.getPhotos().add(photo);
        }
        return urls;
    }

    private MultipartFile resizeImageIfNeeded(MultipartFile file) {
        byte[] originalBytes;
        String originalContentType = file.getContentType();
        try {
            originalBytes = file.getBytes();
            if (originalBytes.length == 0) return file;
        } catch (IOException e) {
            return file;
        }

        try (InputStream is = new java.io.ByteArrayInputStream(originalBytes)) {
            ImageIO.scanForPlugins();
            BufferedImage originalImage = ImageIO.read(is);
            
            if (originalImage == null) {
                return createMultipartFile(file, originalBytes, originalContentType);
            }

            int originWidth = originalImage.getWidth();
            int originHeight = originalImage.getHeight();
            int targetWidth = originWidth;
            int targetHeight = originHeight;

            if (originWidth > MAX_IMAGE_WIDTH) {
                targetWidth = MAX_IMAGE_WIDTH;
                targetHeight = (originHeight * MAX_IMAGE_WIDTH) / originWidth;
            }
            
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = outputImage.createGraphics();
            g2d.setPaint(Color.WHITE); 
            g2d.fillRect(0, 0, targetWidth, targetHeight);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(outputImage, "jpg", baos);
            byte[] processedBytes = baos.toByteArray();

            return createMultipartFile(file, processedBytes, "image/jpeg");
        } catch (Exception e) {
            return createMultipartFile(file, originalBytes, originalContentType);
        }
    }

    private MultipartFile createMultipartFile(MultipartFile original, byte[] bytes, String contentType) {
        return new MultipartFile() {
            @Override public String getName() { return original.getName(); }
            @Override public String getOriginalFilename() { return original.getOriginalFilename(); }
            @Override public String getContentType() { return contentType; }
            @Override public boolean isEmpty() { return bytes.length == 0; }
            @Override public long getSize() { return bytes.length; }
            @Override public byte[] getBytes() { return bytes; }
            @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
            @Override public void transferTo(java.io.File dest) throws IOException, IllegalStateException { 
                java.nio.file.Files.write(dest.toPath(), bytes); 
            }
        };
    }

    public void deletePhoto(Long invitationId, Long photoId) {
        WeddingPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다."));
        if (!photo.getInvitation().getId().equals(invitationId)) {
            throw new IllegalArgumentException("해당 청첩장의 사진이 아닙니다.");
        }
        invitationRepository.findById(invitationId).ifPresent(inv -> inv.getPhotos().remove(photo));
    }

    // ===================== 공개: 청첩장 조회 =====================

    @Transactional
    public WeddingInvitationResponseDto getBySlug(String slug) {
        WeddingInvitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("청첩장을 찾을 수 없습니다."));

        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            throw new IllegalStateException("공개된 청첩장이 아닙니다.");
        }

        invitation.incrementViewCount();
        return new WeddingInvitationResponseDto(invitation);
    }

    // ===================== 공개: RSVP =====================

    public void submitRsvp(String slug, WeddingRsvpRequestDto dto) {
        WeddingInvitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("청첩장을 찾을 수 없습니다."));

        WeddingRsvp rsvp = WeddingRsvp.builder()
                .invitation(invitation)
                .name(dto.getName())
                .phone(dto.getPhone())
                .attendance(dto.getAttendance())
                .guestCount(dto.getGuestCount() > 0 ? dto.getGuestCount() : 1)
                .mealOption(dto.getMealOption())
                .side(dto.getSide() != null ? dto.getSide() : RsvpSide.UNKNOWN)
                .message(dto.getMessage())
                .build();

        rsvpRepository.save(rsvp);
    }

    @Transactional(readOnly = true)
    public WeddingRsvpSummaryDto getRsvpSummary(Long invitationId) {
        List<WeddingRsvp> rsvps = rsvpRepository.findByInvitationIdOrderByCreatedAtDesc(invitationId);
        return new WeddingRsvpSummaryDto(rsvps);
    }

    // ===================== 공개: 방명록 =====================

    public WeddingGuestbookResponseDto writeGuestbook(String slug, WeddingGuestbookRequestDto dto) {
        WeddingInvitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("청첩장을 찾을 수 없습니다."));

        WeddingGuestbook guestbook = WeddingGuestbook.builder()
                .invitation(invitation)
                .name(dto.getName())
                .password(dto.getPassword())
                .message(dto.getMessage())
                .build();

        return new WeddingGuestbookResponseDto(guestbookRepository.save(guestbook));
    }

    @Transactional(readOnly = true)
    public List<WeddingGuestbookResponseDto> getGuestbook(String slug) {
        WeddingInvitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("청첩장을 찾을 수 없습니다."));

        return guestbookRepository
                .findByInvitationIdAndHiddenFalseOrderByCreatedAtDesc(invitation.getId())
                .stream()
                .map(WeddingGuestbookResponseDto::new)
                .collect(Collectors.toList());
    }

    public void deleteGuestbook(Long guestbookId, String password) {
        WeddingGuestbook guestbook = guestbookRepository.findById(guestbookId)
                .orElseThrow(() -> new NoSuchElementException("방명록을 찾을 수 없습니다."));
        if (!guestbook.getPassword().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        guestbook.hide();
    }

    public void hideGuestbook(Long guestbookId) {
        WeddingGuestbook guestbook = guestbookRepository.findById(guestbookId)
                .orElseThrow(() -> new NoSuchElementException("방명록을 찾을 수 없습니다."));
        guestbook.hide();
    }

    // ===================== 내부 헬퍼 =====================

    private WeddingInvitation getInvitationById(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("청첩장을 찾을 수 없습니다. id=" + id));
    }

    private void saveAccounts(WeddingInvitation invitation, List<WeddingInvitationRequestDto.AccountDto> accountDtos) {
        if (accountDtos == null || accountDtos.isEmpty()) return;
        for (WeddingInvitationRequestDto.AccountDto dto : accountDtos) {
            WeddingAccount account = WeddingAccount.builder()
                    .invitation(invitation)
                    .side(dto.getSide())
                    .bankName(dto.getBankName())
                    .accountNumber(dto.getAccountNumber())
                    .accountHolder(dto.getAccountHolder())
                    .kakaoPayLink(dto.getKakaoPayLink())
                    .build();
            invitation.getAccounts().add(account);
        }
    }
}
