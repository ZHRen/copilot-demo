package com.example.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileSearchController.class)
@TestPropertySource(properties = "app.upload-dir=${java.io.tmpdir}/copilot-demo-test-search")
class FileSearchControllerTest {

    private static final Path SEARCH_DIR =
            Path.of(System.getProperty("java.io.tmpdir"), "copilot-demo-test-search");

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(SEARCH_DIR);
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var entries = Files.list(SEARCH_DIR)) {
            for (Path entry : entries.toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    @Test
    void searchReturnsEmptyListWhenNoFiles() throws Exception {
        mockMvc.perform(get("/api/files/search").param("keyword", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchReturnsMatchingFiles() throws Exception {
        Files.writeString(SEARCH_DIR.resolve("report_2024.txt"), "data");
        Files.writeString(SEARCH_DIR.resolve("summary.txt"), "data");

        mockMvc.perform(get("/api/files/search").param("keyword", "report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("report_2024.txt"));
    }

    @Test
    void searchWithNoKeywordReturnsAllFiles() throws Exception {
        Files.writeString(SEARCH_DIR.resolve("alpha.txt"), "data");
        Files.writeString(SEARCH_DIR.resolve("beta.txt"), "data");

        mockMvc.perform(get("/api/files/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
