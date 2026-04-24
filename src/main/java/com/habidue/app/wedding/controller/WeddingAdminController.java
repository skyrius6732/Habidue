package com.habidue.app.wedding.controller;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.wedding.dto.*;
import com.habidue.app.wedding.service.WeddingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/wedding")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class WeddingAdminController {

    private final WeddingService weddingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WeddingInvitationResponseDto>>> getAll() {
        return ApiResponse.success(weddingService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WeddingInvitationResponseDto>> getById(@PathVariable Long id) {
        return ApiResponse.success(weddingService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WeddingInvitationResponseDto>> create(
            @RequestPart("request") @Valid WeddingInvitationRequestDto request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        return ApiResponse.success(HttpStatus.CREATED, weddingService.create(request, files));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WeddingInvitationResponseDto>> update(
            @PathVariable Long id,
            @RequestPart("request") @Valid WeddingInvitationRequestDto request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        return ApiResponse.success(weddingService.update(id, request, files));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        weddingService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/music")
    public ResponseEntity<ApiResponse<String>> uploadMusic(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.success(weddingService.uploadMusic(id, file));
    }

    @PostMapping("/{id}/photos")
    public ResponseEntity<ApiResponse<List<String>>> uploadPhotos(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) throws IOException {
        return ApiResponse.success(weddingService.uploadPhotos(id, files));
    }

    @PutMapping("/{id}/photos/order")
    public ResponseEntity<ApiResponse<Void>> updatePhotoOrder(
            @PathVariable Long id,
            @RequestBody List<Long> photoIds) {
        weddingService.updatePhotoOrder(id, photoIds);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{invitationId}/photos/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @PathVariable Long invitationId,
            @PathVariable Long photoId) {
        weddingService.deletePhoto(invitationId, photoId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/rsvp")
    public ResponseEntity<ApiResponse<WeddingRsvpSummaryDto>> getRsvp(@PathVariable Long id) {
        return ApiResponse.success(weddingService.getRsvpSummary(id));
    }

    @DeleteMapping("/guestbook/{guestbookId}/hide")
    public ResponseEntity<ApiResponse<Void>> hideGuestbook(@PathVariable Long guestbookId) {
        weddingService.hideGuestbook(guestbookId);
        return ApiResponse.success(null);
    }
}
