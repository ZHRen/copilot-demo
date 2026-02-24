package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileDownloadController.class)
@TestPropertySource(properties = {
        "app.upload-dir=${java.io.tmpdir}/copilot-demo-rate-limit-test",
        "app.rate-limit.max-requests=2",
        "app.rate-limit.window-seconds=60"
})
class RateLimitInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestsExceedingRateLimitReturn429() throws Exception {
        mockMvc.perform(get("/api/files/download").param("filename", "a.txt"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/files/download").param("filename", "a.txt"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/files/download").param("filename", "a.txt"))
                .andExpect(status().isTooManyRequests());
    }
}
