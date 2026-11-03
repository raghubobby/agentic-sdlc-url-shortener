package com.agentic.sdlc.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerH2Test {
    @Autowired
    MockMvc mvc;

    @Test
    void generatedCodesUseSevenCharacterBase62Default() throws Exception {
        mvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/generated\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("[0-9A-Za-z]{7}")));
    }

    @Test
    void runsEndToEndWithZeroExternalInfrastructure() throws Exception {
        mvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/h2\",\"customAlias\":\"h2demo1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("h2demo1"));

        mvc.perform(get("/r/h2demo1").header("User-Agent", "curl/8.0"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/h2"));

        mvc.perform(get("/api/v1/urls/h2demo1/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(1));

        mvc.perform(get("/api/v1/urls/h2demo1/analytics/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].userAgentFamily").value("curl"));
    }

    @Test
    void expirationContractAcceptsFutureAndRejectsPastTimestamps() throws Exception {
        mvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/future\",\"customAlias\":\"future1\",\"expiresAt\":\"2099-12-31T23:59:59Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").value("2099-12-31T23:59:59Z"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/past\",\"expiresAt\":\"2000-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void idempotencyReplaysSameRequestAndRejectsKeyReuseForDifferentPayload() throws Exception {
        String payload = "{\"url\":\"https://example.com/idempotent\",\"customAlias\":\"idemh2\"}";

        mvc.perform(post("/api/v1/urls")
                        .header("Idempotency-Key", "h2-idempotency-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("idemh2"));

        mvc.perform(post("/api/v1/urls")
                        .header("Idempotency-Key", "h2-idempotency-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("idemh2"));

        mvc.perform(post("/api/v1/urls")
                        .header("Idempotency-Key", "h2-idempotency-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.org/different\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void customAliasValidationRejectsInvalidAndDuplicateAliases() throws Exception {
        mvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/one\",\"customAlias\":\"memorable\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("memorable"));

        mvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/two\",\"customAlias\":\"memorable\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOM_ALIAS_TAKEN"));

        mvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/three\",\"customAlias\":\"bad alias\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
