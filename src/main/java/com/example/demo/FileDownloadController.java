package com.example.demo;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileDownloadController {

    private final Path uploadDir;
    private final int rateLimit;
    // Maximum number of IP entries to retain; evicts the least-recently-used entry once exceeded.
    private static final int MAX_BUCKETS = 10_000;
    private final Map<String, Bucket> buckets = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    return size() > MAX_BUCKETS;
                }
            });

    public FileDownloadController(
            @Value("${app.upload-dir:uploads}") String uploadDirPath,
            @Value("${app.download.rate-limit:10}") int rateLimit) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        this.rateLimit = rateLimit;
    }

    private Bucket resolveBucket(String clientIp) {
        synchronized (buckets) {
            return buckets.computeIfAbsent(clientIp, ip ->
                    Bucket.builder()
                            .addLimit(Bandwidth.builder()
                                    .capacity(rateLimit)
                                    .refillGreedy(rateLimit, Duration.ofMinutes(1))
                                    .build())
                            .build());
        }
    }

    @GetMapping("/files/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        if (!resolveBucket(clientIp).tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        if (filename == null || filename.isBlank() || !filename.matches("[\\w.\\-]+")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path filePath = uploadDir.resolve(filename).normalize();

            if (!filePath.startsWith(uploadDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
