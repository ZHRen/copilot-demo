package com.example.demo;

import com.example.demo.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileUploadController.class)
@TestPropertySource(properties = "app.storage.max-file-size-bytes=1024")
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @Test
    void uploadFileSuccess() throws Exception {
        when(storageService.store(any())).thenReturn("abc123_test.txt");

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello world".getBytes());

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("abc123_test.txt"));
    }

    @Test
    void uploadEmptyFileRejected() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/files/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void uploadFileTooLargeRejected() throws Exception {
        byte[] oversized = new byte[2048]; // exceeds the 1024 byte limit set in @TestPropertySource
        MockMultipartFile file = new MockMultipartFile("file", "big.txt", "text/plain", oversized);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void uploadFileStorageFailureReturns500() throws Exception {
        when(storageService.store(any())).thenThrow(new java.io.UncheckedIOException(
                "disk full", new java.io.IOException("disk full")));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Storage error occurred"));
    }

    @Test
    void uploadFileIllegalArgumentReturns400() throws Exception {
        when(storageService.store(any())).thenThrow(
                new IllegalArgumentException("Filename contains invalid characters"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "bad\0file.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Filename contains invalid characters"));
    }

    @Test
    void uploadFileWithPathTraversalFilenameReturns400() throws Exception {
        when(storageService.store(any())).thenThrow(
                new IllegalArgumentException("Invalid filename: ../../../etc/passwd"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "../../../etc/passwd", "text/plain", "secret".getBytes());

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
