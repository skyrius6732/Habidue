package com.habidue.app.service.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "s3")
@RequiredArgsConstructor
public class S3FileService implements FileStorageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Override
    public List<String> upload(List<MultipartFile> files) throws IOException {
        return upload(files, "posts");
    }

    @Override
    public List<String> upload(List<MultipartFile> files, String subfolder) throws IOException {
        List<String> urls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            String ext = original.substring(original.lastIndexOf("."));
            String key = subfolder + "/" + UUID.randomUUID() + ext;

            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build(),
                RequestBody.fromBytes(file.getBytes())
            );

            urls.add("https://" + bucket + ".s3." + region + ".amazonaws.com/" + key);
        }
        return urls;
    }
}
