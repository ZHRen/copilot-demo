package com.example.demo;

import com.example.demo.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.UncheckedIOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final StorageService storageService;
    private final long maxFileSizeBytes;

    public FileUploadController(StorageService storageService,
            @Value("${app.storage.max-file-size-bytes:104857600}") long maxFileSizeBytes) {
        this.storageService = storageService;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File must not be empty"));
        }
        if (file.getSize() > maxFileSizeBytes) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "File size exceeds the maximum allowed limit of " + maxFileSizeBytes + " bytes"));
        }

        String storedName = storageService.store(file);
        return ResponseEntity.ok(Map.of("filename", storedName));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<Map<String, String>> handleStorageError(UncheckedIOException ex) {
        return ResponseEntity.internalServerError().body(Map.of("error", "Storage error occurred"));
    }
}
