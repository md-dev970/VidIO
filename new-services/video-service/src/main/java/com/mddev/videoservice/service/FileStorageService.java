package com.mddev.videoservice.service;

import com.mddev.videoservice.exception.StorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path originalDirectory;

    public FileStorageService(@Value("${video.storage.base-path}") String basePath) {
        this.originalDirectory = Path.of(basePath).toAbsolutePath().normalize().resolve("original");
    }

    public String saveOriginal(UUID videoId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }

        try {
            Files.createDirectories(originalDirectory);
            String filename = videoId + extensionFor(file.getOriginalFilename());
            Path destination = originalDirectory.resolve(filename).normalize();
            file.transferTo(destination);
            return destination.toString();
        } catch (IOException exception) {
            throw new StorageException("Failed to store uploaded video", exception);
        }
    }

    private String extensionFor(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleaned.length() - 1) {
            return ".mp4";
        }
        return cleaned.substring(dotIndex);
    }
}
