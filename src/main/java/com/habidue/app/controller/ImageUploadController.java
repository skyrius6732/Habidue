package com.habidue.app.controller;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        try {
            List<String> imageUrls = fileStorageService.upload(files);
            return ApiResponse.success(imageUrls);
        } catch (IOException e) {
            return ApiResponse.error(null, "이미지 업로드 실패: " + e.getMessage(), "Upload Error");
        }
    }
}
