package com.example.demo.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * StorageService implementation for local filesystem and NAS (Network Attached Storage).
 * NAS volumes are mounted as a local path, so both types share the same implementation.
 */
public class FileSystemStorageService implements StorageService {

    private final Path baseDir;

    public FileSystemStorageService(String baseDirPath) {
        this.baseDir = Paths.get(baseDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create storage directory: " + baseDirPath, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "upload";
        }
        Path filenamePath = Paths.get(originalFilename).getFileName();
        String safeFilename = StorageUtils.sanitizeFilename(filenamePath != null ? filenamePath.toString() : "upload");
        String storedFilename = UUID.randomUUID() + "_" + safeFilename;

        Path targetPath = baseDir.resolve(storedFilename).normalize();
        if (!targetPath.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid filename: " + originalFilename);
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file: " + storedFilename, e);
        }
        return storedFilename;
    }
}
