package com.habidue.app.wedding.controller;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.wedding.dto.*;
import com.habidue.app.wedding.service.WeddingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wedding")
@RequiredArgsConstructor
public class WeddingPublicController {

    private final WeddingService weddingService;

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<WeddingInvitationResponseDto>> getBySlug(
            @PathVariable String slug) {
        return ApiResponse.success(weddingService.getBySlug(slug));
    }

    @PostMapping("/{slug}/rsvp")
    public ResponseEntity<ApiResponse<Void>> submitRsvp(
            @PathVariable String slug,
            @Valid @RequestBody WeddingRsvpRequestDto request) {
        weddingService.submitRsvp(slug, request);
        return ApiResponse.success(HttpStatus.CREATED, null);
    }

    @GetMapping("/{slug}/guestbook")
    public ResponseEntity<ApiResponse<List<WeddingGuestbookResponseDto>>> getGuestbook(
            @PathVariable String slug) {
        return ApiResponse.success(weddingService.getGuestbook(slug));
    }

    @PostMapping("/{slug}/guestbook")
    public ResponseEntity<ApiResponse<WeddingGuestbookResponseDto>> writeGuestbook(
            @PathVariable String slug,
            @Valid @RequestBody WeddingGuestbookRequestDto request) {
        return ApiResponse.success(HttpStatus.CREATED, weddingService.writeGuestbook(slug, request));
    }

    @DeleteMapping("/guestbook/{guestbookId}")
    public ResponseEntity<ApiResponse<Void>> deleteGuestbook(
            @PathVariable Long guestbookId,
            @RequestParam String password) {
        weddingService.deleteGuestbook(guestbookId, password);
        return ApiResponse.success(null);
    }
}
