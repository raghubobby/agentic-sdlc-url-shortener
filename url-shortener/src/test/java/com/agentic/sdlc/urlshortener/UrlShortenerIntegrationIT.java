package com.agentic.sdlc.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("postgres")
class UrlShortenerIntegrationIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("aiassist")
            .withUsername("aiassist")
            .withPassword("aiassist");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl() + "?currentSchema=url_shortener");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("url-shortener.base-url", () -> "https://sho.rt/r");
        registry.add("url-shortener.cache-enabled", () -> "true");
    }

    @Autowired
    MockMvc mvc;

    @Test
    void createReplayRedirectAnalyticsAndDeactivateEndToEnd() throws Exception {
        String create = """
                {"url":"https://example.com/docs","customAlias":"staffdemo"}
                """;

        mvc.perform(post("/api/v1/urls")
                        .header("Idempotency-Key", "demo-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(create))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("staffdemo"))
                .andExpect(jsonPath("$.shortUrl").value("https://sho.rt/r/staffdemo"));

        mvc.perform(post("/api/v1/urls")
                        .header("Idempotency-Key", "demo-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(create))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("staffdemo"));

        mvc.perform(post("/api/v1/urls")
                        .header("Idempotency-Key", "demo-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.org/different\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        mvc.perform(get("/r/staffdemo")
                        .header("User-Agent", "curl/8.0"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/docs"));

        mvc.perform(get("/api/v1/urls/staffdemo/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(1))
                .andExpect(jsonPath("$.browserFamilies[0].value").value("curl"));

        mvc.perform(get("/api/v1/urls/staffdemo/analytics/events?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(1))
                .andExpect(jsonPath("$.events[0].userAgentFamily").value("curl"));

        mvc.perform(delete("/api/v1/urls/staffdemo"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/r/staffdemo"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SHORT_URL_INACTIVE"));
    }

    @Test
    void rejectsLocalNetworkDestinations() throws Exception {
        mvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://127.0.0.1/admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
