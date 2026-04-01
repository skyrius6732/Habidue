package com.habidue.app.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "local", matchIfMissing = true)
public class LocalFileService implements FileStorageService {

    @Value("${file.upload-dir:/home/skyrius/habiDue/uploads/posts}")
    private String uploadDir;

    @Override
    public List<String> upload(List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            String ext = original.substring(original.lastIndexOf("."));
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), Paths.get(uploadDir, filename));
            urls.add("/uploads/posts/" + filename);
        }
        return urls;
    }
}
