package com.example.demo.storage;

import com.obs.services.ObsClient;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PutObjectRequest;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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

    /** Package-visible constructor for testing — accepts a pre-built ObsClient. */
    ObsStorageService(ObsClient obsClient, String bucketName) {
        this.obsClient = obsClient;
        this.bucketName = bucketName;
    }

    @Override
    public String store(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "upload";
        }
        Path filenamePath = Paths.get(originalFilename).getFileName();
        String safeFilename = StorageUtils.sanitizeFilename(filenamePath != null ? filenamePath.toString() : "upload");
        String objectKey = UUID.randomUUID() + "_" + safeFilename;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            metadata.setContentType(contentType);
        }

        PutObjectRequest request = new PutObjectRequest(bucketName, objectKey);
        request.setMetadata(metadata);
        try (InputStream inputStream = file.getInputStream()) {
            request.setInput(inputStream);
            try {
                obsClient.putObject(request);
            } catch (RuntimeException e) {
                throw new UncheckedIOException("Failed to upload file to OBS", new IOException(e.getMessage(), e));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
        return objectKey;
    }

    @Override
    public void destroy() throws Exception {
        obsClient.close();
    }
}
