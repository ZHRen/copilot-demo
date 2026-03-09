package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class FileSearchController {

    private static final Logger logger = LoggerFactory.getLogger(FileSearchController.class);

    private final Path uploadDir;

    public FileSearchController(@Value("${app.upload-dir:uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    @GetMapping("/files/search")
    public ResponseEntity<List<String>> searchFiles(
            @RequestParam(required = false, defaultValue = "") String keyword) {

        if (!Files.isDirectory(uploadDir)) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        try (Stream<Path> entries = Files.list(uploadDir)) {
            List<String> results = entries
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> keyword.isEmpty() || name.contains(keyword))
                    .sorted()
                    .toList();
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            logger.error("Failed to list files in upload directory: {}", uploadDir, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
