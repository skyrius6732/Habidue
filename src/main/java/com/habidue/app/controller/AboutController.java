package com.habidue.app.controller;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.about.*;
import com.habidue.app.service.about.AboutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announce-patch")
@RequiredArgsConstructor
public class AboutController {

    private final AboutService aboutService;

    // --- 공지사항 API ---
    @GetMapping("/notices")
    public ResponseEntity<ApiResponse<List<AnnouncementResponseDto>>> getAnnouncements() {
        return ApiResponse.success(aboutService.getAnnouncements());
    }

    @PostMapping("/notices")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<AnnouncementResponseDto>> createAnnouncement(@RequestBody AnnouncementRequestDto dto) {
        return ApiResponse.success(HttpStatus.CREATED, aboutService.createAnnouncement(dto));
    }

    @PutMapping("/notices/{id}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<AnnouncementResponseDto>> updateAnnouncement(@PathVariable Long id, @RequestBody AnnouncementRequestDto dto) {
        return ApiResponse.success(aboutService.updateAnnouncement(id, dto));
    }

    @DeleteMapping("/notices/{id}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable Long id) {
        aboutService.deleteAnnouncement(id);
        return ApiResponse.success(HttpStatus.NO_CONTENT, null);
    }


    // --- 패치 노트 API ---
    @GetMapping("/patches")
    public ResponseEntity<ApiResponse<List<PatchNoteResponseDto>>> getPatchNotes() {
        return ApiResponse.success(aboutService.getPatchNotes());
    }

    @PostMapping("/patches")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<PatchNoteResponseDto>> createPatchNote(@RequestBody PatchNoteRequestDto dto) {
        return ApiResponse.success(HttpStatus.CREATED, aboutService.createPatchNote(dto));
    }

    @PutMapping("/patches/{id}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<PatchNoteResponseDto>> updatePatchNote(@PathVariable Long id, @RequestBody PatchNoteRequestDto dto) {
        return ApiResponse.success(aboutService.updatePatchNote(id, dto));
    }

    @DeleteMapping("/patches/{id}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> deletePatchNote(@PathVariable Long id) {
        aboutService.deletePatchNote(id);
        return ApiResponse.success(HttpStatus.NO_CONTENT, null);
    }
}
