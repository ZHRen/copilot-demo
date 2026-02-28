package com.example.demo.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

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

    @Override
    public List<FileInfo> search(String keyword) {
        try (Stream<Path> files = Files.list(baseDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> matchesKeyword(p.getFileName().toString(), keyword))
                    .map(this::toFileInfo)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list files in storage directory", e);
        }
    }

    private static boolean matchesKeyword(String filename, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return filename.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private FileInfo toFileInfo(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return new FileInfo(
                    path.getFileName().toString(),
                    attrs.size(),
                    attrs.lastModifiedTime().toInstant().toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file attributes: " + path.getFileName(), e);
        }
    }
}
