package com.example.demo;

import com.example.demo.storage.FileInfo;
import com.example.demo.storage.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileQueryController {

    private final StorageService storageService;

    public FileQueryController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Search for uploaded files. If {@code keyword} is provided, only files whose name
     * contains the keyword (case-insensitive) are returned. Otherwise all files are listed.
     */
    @GetMapping("/files")
    public ResponseEntity<List<FileInfo>> searchFiles(
            @RequestParam(required = false) String keyword) {
        List<FileInfo> files = storageService.search(keyword);
        return ResponseEntity.ok(files);
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<Map<String, String>> handleStorageError(UncheckedIOException ex) {
        return ResponseEntity.internalServerError().body(Map.of("error", "Storage error occurred"));
    }
}
