package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RepositoryController.class)
class RepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createRepositorySuccess() throws Exception {
        String requestBody = "{\"name\":\"test-repo\",\"description\":\"A test repository\"}";

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test-repo"))
                .andExpect(jsonPath("$.description").value("A test repository"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createRepositoryWithoutDescription() throws Exception {
        String requestBody = "{\"name\":\"test-repo\"}";

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test-repo"));
    }

    @Test
    void createRepositoryWithEmptyNameRejected() throws Exception {
        String requestBody = "{\"name\":\"\",\"description\":\"A test repository\"}";

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRepositoryWithoutNameRejected() throws Exception {
        String requestBody = "{\"description\":\"A test repository\"}";

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
