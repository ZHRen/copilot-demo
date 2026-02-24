package com.example.demo.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Store the given file and return the relative path/key under which it was saved.
     */
    String store(MultipartFile file);
}
