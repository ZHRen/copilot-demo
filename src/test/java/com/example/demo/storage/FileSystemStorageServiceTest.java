package com.example.demo.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void searchReturnsAllFilesWhenKeywordIsNull() throws IOException {
        FileSystemStorageService service = new FileSystemStorageService(tempDir.toString());
        Files.writeString(tempDir.resolve("uuid1_alpha.txt"), "a");
        Files.writeString(tempDir.resolve("uuid2_beta.txt"), "b");

        List<FileInfo> results = service.search(null);

        assertEquals(2, results.size());
    }

    @Test
    void searchFiltersByKeywordCaseInsensitive() throws IOException {
        FileSystemStorageService service = new FileSystemStorageService(tempDir.toString());
        Files.writeString(tempDir.resolve("uuid1_Report.csv"), "data");
        Files.writeString(tempDir.resolve("uuid2_image.png"), "img");

        List<FileInfo> results = service.search("report");

        assertEquals(1, results.size());
        assertEquals("uuid1_Report.csv", results.get(0).filename());
    }

    @Test
    void searchReturnsEmptyListWhenNoMatch() throws IOException {
        FileSystemStorageService service = new FileSystemStorageService(tempDir.toString());
        Files.writeString(tempDir.resolve("uuid1_document.pdf"), "doc");

        List<FileInfo> results = service.search("nonexistent");

        assertTrue(results.isEmpty());
    }

    @Test
    void searchReturnsCorrectFileMetadata() throws IOException {
        FileSystemStorageService service = new FileSystemStorageService(tempDir.toString());
        byte[] content = "hello".getBytes();
        Files.write(tempDir.resolve("uuid_file.txt"), content);

        List<FileInfo> results = service.search(null);

        assertEquals(1, results.size());
        FileInfo info = results.get(0);
        assertEquals("uuid_file.txt", info.filename());
        assertEquals(content.length, info.sizeBytes());
        assertNotNull(info.lastModified());
    }
}
