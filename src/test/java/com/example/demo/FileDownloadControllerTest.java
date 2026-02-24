package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileDownloadController.class)
@TestPropertySource(properties = "app.upload-dir=${java.io.tmpdir}/copilot-demo-test-uploads")
class FileDownloadControllerTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void downloadFileNotFound() throws Exception {
        mockMvc.perform(get("/api/files/download").param("filename", "nonexistent.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadFileInvalidNameRejected() throws Exception {
        mockMvc.perform(get("/api/files/download").param("filename", "../secret.txt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadFileSuccess() throws Exception {
        Path uploadsDir = Path.of(System.getProperty("java.io.tmpdir"), "copilot-demo-test-uploads");
        Files.createDirectories(uploadsDir);
        Path testFile = uploadsDir.resolve("test.txt");
        Files.writeString(testFile, "hello");
        try {
            mockMvc.perform(get("/api/files/download").param("filename", "test.txt"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Disposition"));
        } finally {
            Files.deleteIfExists(testFile);
        }
    }
}
