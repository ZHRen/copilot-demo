package com.example.demo.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {

    /**
     * Store the given file and return the relative path/key under which it was saved.
     */
    String store(MultipartFile file);

    /**
     * Search for stored files whose name contains the given keyword (case-insensitive).
     * If {@code keyword} is null or blank, all files are returned.
     */
    List<FileInfo> search(String keyword);
}
