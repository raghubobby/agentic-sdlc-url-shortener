package com.agentic.sdlc.urlshortener.service;

import com.agentic.sdlc.urlshortener.api.CreateShortUrlRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class RequestHasher {
    public String hash(CreateShortUrlRequest request) {
        String canonical = request.url().trim() + "\n" +
                normalize(request.customAlias()) + "\n" +
                normalize(request.expiresAt());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(Instant value) {
        return value == null ? "" : value.toString();
    }
}
