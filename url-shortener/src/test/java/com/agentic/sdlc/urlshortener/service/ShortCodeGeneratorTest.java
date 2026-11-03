package com.agentic.sdlc.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTest {
    @Test
    void producesUrlSafeCodesOfRequestedLength() {
        ShortCodeGenerator generator = new ShortCodeGenerator();
        Set<String> values = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String value = generator.generate(8);
            assertThat(value).matches("[A-Za-z0-9]{8}");
            values.add(value);
        }
        assertThat(values).hasSize(500);
    }
}
