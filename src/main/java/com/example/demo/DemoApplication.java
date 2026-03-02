package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the copilot-demo Spring Boot application.
 *
 * <p>This application exposes two REST API endpoints:
 * <ul>
 *   <li>{@code GET /api/health} – returns the application health status.</li>
 *   <li>{@code GET /api/files/download?filename=&lt;name&gt;} – serves files from the configured upload directory.</li>
 * </ul>
 *
 * <p>A per-IP rate limiter ({@link RateLimitInterceptor}) is applied to the file-download endpoint to
 * prevent abuse. Configuration is read from {@code application.properties}.
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * Bootstraps the Spring Boot application context and starts the embedded web server.
     *
     * @param args command-line arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
