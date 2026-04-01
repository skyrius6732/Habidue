package com.habidue.app.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileStorageService {
    List<String> upload(List<MultipartFile> files) throws IOException;
}
