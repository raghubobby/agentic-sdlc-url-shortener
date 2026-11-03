package com.agentic.sdlc.urlshortener.service;

import com.agentic.sdlc.urlshortener.api.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TargetUrlValidatorTest {
    private final TargetUrlValidator validator = new TargetUrlValidator();

    @Test
    void acceptsPublicHttpAndHttpsTargets() {
        assertThat(validator.validateAndNormalize(" https://example.com/a/../docs?q=1 "))
                .isEqualTo("https://example.com/docs?q=1");
        assertThat(validator.validateAndNormalize("http://example.com"))
                .isEqualTo("http://example.com");
    }

    @Test
    void rejectsUnsafeSchemesCredentialsAndLocalTargets() {
        assertThatThrownBy(() -> validator.validateAndNormalize("javascript:alert(1)"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("https://user:secret@example.com"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("http://127.0.0.1/admin"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("http://localhost:8080"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("http://10.1.2.3/internal"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
