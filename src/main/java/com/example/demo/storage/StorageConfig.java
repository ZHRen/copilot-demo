package com.example.demo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the appropriate {@link StorageService} bean based on {@code app.storage.type}.
 * Supported values: {@code local}, {@code nas}, {@code obs}.
 */
@Configuration
public class StorageConfig {

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.local.base-dir:uploads}")
    private String localBaseDir;

    @Value("${app.storage.nas.base-dir:}")
    private String nasBaseDir;

    @Value("${app.storage.obs.endpoint:}")
    private String obsEndpoint;

    @Value("${app.storage.obs.access-key:}")
    private String obsAccessKey;

    @Value("${app.storage.obs.secret-key:}")
    private String obsSecretKey;

    @Value("${app.storage.obs.bucket-name:}")
    private String obsBucketName;

    @Bean
    public StorageService storageService() {
        return switch (storageType.toLowerCase()) {
            case "nas" -> {
                if (nasBaseDir == null || nasBaseDir.isBlank()) {
                    throw new IllegalArgumentException(
                            "NAS base directory must not be blank (app.storage.nas.base-dir)");
                }
                yield new FileSystemStorageService(nasBaseDir);
            }
            case "obs" -> new ObsStorageService(obsEndpoint, obsAccessKey, obsSecretKey, obsBucketName);
            default -> new FileSystemStorageService(localBaseDir);
        };
    }
}
