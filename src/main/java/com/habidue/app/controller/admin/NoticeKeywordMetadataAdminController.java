package com.habidue.app.controller.admin;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.notice.NoticeKeywordMetadataRequestDto;
import com.habidue.app.dto.notice.NoticeKeywordMetadataResponseDto;
import com.habidue.app.service.notice.NoticeKeywordMetadataAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/metadata")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class NoticeKeywordMetadataAdminController {

    private final NoticeKeywordMetadataAdminService metadataService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeKeywordMetadataResponseDto>>> getAllMetadata() {
        return ApiResponse.success(metadataService.getAllMetadata());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NoticeKeywordMetadataResponseDto>> createMetadata(
            @RequestBody NoticeKeywordMetadataRequestDto dto) {
        return ApiResponse.success(metadataService.createMetadata(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeKeywordMetadataResponseDto>> updateMetadata(
            @PathVariable Long id,
            @RequestBody NoticeKeywordMetadataRequestDto dto) {
        return ApiResponse.success(metadataService.updateMetadata(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMetadata(@PathVariable Long id) {
        metadataService.deleteMetadata(id);
        return ApiResponse.success(null);
    }
}
