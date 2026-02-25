package com.example.demo.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeCreatesFileWithUuidPrefix() {
        FileSystemStorageService service = new FileSystemStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "content".getBytes());

        String storedName = service.store(file);

        assertTrue(storedName.endsWith("_hello.txt"), "Stored name should end with _hello.txt but was: " + storedName);
        assertTrue(Files.exists(tempDir.resolve(storedName)));
    }

    @Test
    void storeWithNullOriginalFilenameUsesUpload() {
        FileSystemStorageService service = new FileSystemStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", null, "text/plain", "content".getBytes());

        String storedName = service.store(file);

        assertTrue(storedName.endsWith("_upload"), "Should fall back to 'upload' for null filename");
    }

    @Test
    void storeWithAllSpecialCharsFilenameUsesUploadExtension() {
        FileSystemStorageService service = new FileSystemStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "!@#$.txt", "text/plain", "content".getBytes());

        String storedName = service.store(file);

        assertTrue(storedName.endsWith("_upload.txt"), "Should replace all-special base with 'upload'");
    }

    @Test
    void storeWithNullByteFilenameThrowsIllegalArgument() {
        FileSystemStorageService service = new FileSystemStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "bad\0name.txt", "text/plain", "content".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.store(file));
    }

    @Test
    void constructorCreatesBaseDirectory() throws IOException {
        Path subDir = tempDir.resolve("new-uploads");
        assertFalse(Files.exists(subDir));

        new FileSystemStorageService(subDir.toString());

        assertTrue(Files.isDirectory(subDir));
    }

    @Test
    void storePreservesFileContent() throws IOException {
        FileSystemStorageService service = new FileSystemStorageService(tempDir.toString());
        byte[] content = "hello world".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content);

        String storedName = service.store(file);

        assertArrayEquals(content, Files.readAllBytes(tempDir.resolve(storedName)));
    }
}
