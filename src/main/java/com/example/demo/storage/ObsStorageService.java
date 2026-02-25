package com.example.demo.storage;

import com.obs.services.ObsClient;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PutObjectRequest;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * StorageService implementation for Huawei Cloud OBS (Object Storage Service).
 */
public class ObsStorageService implements StorageService, DisposableBean {

    private final ObsClient obsClient;
    private final String bucketName;

    public ObsStorageService(String endpoint, String accessKey, String secretKey, String bucketName) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("OBS endpoint must not be blank (app.storage.obs.endpoint)");
        }
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("OBS access key must not be blank (app.storage.obs.access-key)");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("OBS secret key must not be blank (app.storage.obs.secret-key)");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("OBS bucket name must not be blank (app.storage.obs.bucket-name)");
        }
        this.obsClient = new ObsClient(accessKey, secretKey, endpoint);
        this.bucketName = bucketName;
    }

    @Override
    public String store(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "upload";
        }
        Path filenamePath = Paths.get(originalFilename).getFileName();
        String safeFilename = sanitizeFilename(filenamePath != null ? filenamePath.toString() : "upload");
        String objectKey = UUID.randomUUID() + "_" + safeFilename;

        PutObjectRequest request = new PutObjectRequest(bucketName, objectKey);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        request.setMetadata(metadata);
        try {
            request.setInput(file.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
        obsClient.putObject(request);
        return objectKey;
    }

    @Override
    public void destroy() throws Exception {
        obsClient.close();
    }

    private static String sanitizeFilename(String filename) {
        if (filename.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Filename contains invalid characters");
        }
        return filename.replaceAll("[^\\w.\\-]", "_");
    }
}
