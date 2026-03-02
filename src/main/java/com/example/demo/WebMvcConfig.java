package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration that registers application-level interceptors.
 *
 * <p>Currently registered interceptors:
 * <ul>
 *   <li>{@link RateLimitInterceptor} – applied to {@code /api/files/download} to
 *       protect the file-download endpoint from excessive traffic.</li>
 * </ul>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Injects the rate-limit interceptor via constructor injection.
     *
     * @param rateLimitInterceptor the interceptor bean managed by Spring
     */
    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * Registers the {@link RateLimitInterceptor} for the file-download path pattern.
     *
     * @param registry Spring's interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/files/download");
    }
}
