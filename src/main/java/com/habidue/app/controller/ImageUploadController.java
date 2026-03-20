package com.habidue.app.controller;

import com.habidue.app.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageUploadController {

    @Value("${file.upload-dir:/home/skyrius/habiDue/uploads/posts}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();
        
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String savedFilename = UUID.randomUUID().toString() + extension;
            
            Path filePath = Paths.get(uploadDir, savedFilename);
            
            try {
                Files.copy(file.getInputStream(), filePath);
                // 현재는 로컬 서버 경로를 반환 (실제 운영 시 S3 도메인 등 사용)
                imageUrls.add("/uploads/posts/" + savedFilename);
            } catch (IOException e) {
                return ApiResponse.error(null, "이미지 업로드 실패: " + originalFilename, "Upload Error");
            }
        }

        return ApiResponse.success(imageUrls);
    }
}
