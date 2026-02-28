package com.example.demo;

import com.example.demo.storage.FileInfo;
import com.example.demo.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.UncheckedIOException;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileQueryController.class)
class FileQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @Test
    void listAllFilesReturnsOk() throws Exception {
        when(storageService.search(null)).thenReturn(List.of(
                new FileInfo("uuid1_a.txt", 100L, "2024-01-01T00:00:00Z"),
                new FileInfo("uuid2_b.txt", 200L, "2024-01-02T00:00:00Z")));

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].filename").value("uuid1_a.txt"))
                .andExpect(jsonPath("$[0].sizeBytes").value(100))
                .andExpect(jsonPath("$[1].filename").value("uuid2_b.txt"));
    }

    @Test
    void searchByKeywordReturnsFilteredResults() throws Exception {
        when(storageService.search("report")).thenReturn(List.of(
                new FileInfo("uuid3_report.csv", 512L, "2024-01-03T00:00:00Z")));

        mockMvc.perform(get("/api/files").param("keyword", "report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].filename").value("uuid3_report.csv"))
                .andExpect(jsonPath("$[0].sizeBytes").value(512));
    }

    @Test
    void searchWithNoMatchReturnsEmptyList() throws Exception {
        when(storageService.search("nonexistent")).thenReturn(List.of());

        mockMvc.perform(get("/api/files").param("keyword", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void storageErrorReturns500() throws Exception {
        when(storageService.search(null)).thenThrow(
                new UncheckedIOException("disk read error", new java.io.IOException("disk read error")));

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Storage error occurred"));
    }
}
