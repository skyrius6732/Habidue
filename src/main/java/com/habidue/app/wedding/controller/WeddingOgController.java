package com.habidue.app.wedding.controller;

import com.habidue.app.wedding.domain.InvitationStatus;
import com.habidue.app.wedding.domain.WeddingInvitation;
import com.habidue.app.wedding.repository.WeddingInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/w")
@RequiredArgsConstructor
public class WeddingOgController {

    private final WeddingInvitationRepository invitationRepository;

    private static final String DEFAULT_IMAGE = "https://habidue.com/Habidue_512.png";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    @GetMapping("/{slug}")
    public ResponseEntity<String> ogPage(@PathVariable String slug) {
        String title;
        String description;
        String image;
        String canonicalUrl = "https://habidue.com/wedding/" + slug;

        try {
            WeddingInvitation inv = invitationRepository.findBySlug(slug)
                    .filter(i -> i.getStatus() == InvitationStatus.ACTIVE)
                    .orElse(null);

            if (inv != null) {
                title = inv.getGroomName() + " ♥ " + inv.getBrideName() + " 결혼합니다";
                String dateStr = inv.getWeddingDate() != null
                        ? inv.getWeddingDate().format(DATE_FMT) : "";
                description = dateStr + " · " + inv.getVenueName();
                image = (inv.getPhotos() != null && !inv.getPhotos().isEmpty())
                        ? inv.getPhotos().get(0).getImageUrl()
                        : DEFAULT_IMAGE;
            } else {
                title = "모바일 청첩장 | 하비듀";
                description = "소중한 결혼식에 초대합니다.";
                image = DEFAULT_IMAGE;
            }
        } catch (Exception e) {
            title = "모바일 청첩장 | 하비듀";
            description = "소중한 결혼식에 초대합니다.";
            image = DEFAULT_IMAGE;
        }

        String html = "<!DOCTYPE html><html lang=\"ko\"><head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta property=\"og:type\" content=\"website\">" +
                "<meta property=\"og:title\" content=\"" + escape(title) + "\">" +
                "<meta property=\"og:description\" content=\"" + escape(description) + "\">" +
                "<meta property=\"og:image\" content=\"" + image + "\">" +
                "<meta property=\"og:url\" content=\"" + canonicalUrl + "\">" +
                "<meta name=\"twitter:card\" content=\"summary_large_image\">" +
                "<meta name=\"twitter:title\" content=\"" + escape(title) + "\">" +
                "<meta name=\"twitter:description\" content=\"" + escape(description) + "\">" +
                "<meta name=\"twitter:image\" content=\"" + image + "\">" +
                "<script>window.location.replace('" + canonicalUrl + "');</script>" +
                "</head><body></body></html>";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
