package com.example.demo.storage;

/**
 * Shared filename sanitization utilities for storage implementations.
 */
class StorageUtils {

    private StorageUtils() {
    }

    /**
     * Sanitizes a filename by replacing sequences of disallowed characters with a single underscore.
     * If the base name (part before the extension) contains no meaningful characters after sanitization,
     * it is replaced with "upload".
     *
     * @throws IllegalArgumentException if the filename contains a null byte
     */
    static String sanitizeFilename(String filename) {
        if (filename.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Filename contains invalid characters");
        }
        String sanitized = filename.replaceAll("[^\\w.\\-]+", "_");

        // If the base name has no meaningful characters, replace it with "upload"
        int lastDot = sanitized.lastIndexOf('.');
        String baseName = lastDot > 0 ? sanitized.substring(0, lastDot) : sanitized;
        String extension = lastDot > 0 ? sanitized.substring(lastDot) : "";
        if (baseName.replaceAll("_", "").isEmpty()) {
            baseName = "upload";
        }
        return baseName + extension;
    }
}
