package com.example.demo.storage;

/**
 * Metadata for a stored file returned by the search API.
 */
public record FileInfo(String filename, long sizeBytes, String lastModified) {
}
