package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller that exposes an application health-check endpoint.
 *
 * <p>The single endpoint {@code GET /api/health} returns a simple JSON object
 * {@code {"status":"UP"}} with HTTP 200 whenever the application is running and
 * able to serve requests. It is intentionally lightweight so that load balancers
 * and monitoring tools can poll it frequently at negligible cost.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Returns the current health status of the application.
     *
     * @return {@code 200 OK} with body {@code {"status":"UP"}}
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
