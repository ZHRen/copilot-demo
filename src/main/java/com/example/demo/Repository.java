package com.example.demo;

import java.time.Instant;
import java.util.UUID;

public class Repository {
    private final String id;
    private final String name;
    private final String description;
    private final Instant createdAt;

    public Repository(String name, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
