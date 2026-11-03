package com.agentic.sdlc.urlshortener.service;

import com.agentic.sdlc.urlshortener.api.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

@Component
public class TargetUrlValidator {
    public String validateAndNormalize(String rawUrl) {
        final URI uri;
        try {
            uri = URI.create(rawUrl.trim()).normalize();
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("url", "must be a valid absolute URI");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new InvalidRequestException("url", "only http and https URLs are allowed");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidRequestException("url", "a hostname is required");
        }
        if (uri.getUserInfo() != null) {
            throw new InvalidRequestException("url", "embedded credentials are not allowed");
        }
        if (uri.getPort() < -1 || uri.getPort() > 65535) {
            throw new InvalidRequestException("url", "invalid port");
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")) {
            throw new InvalidRequestException("url", "local destinations are not allowed");
        }
        rejectNonPublicLiteralAddress(host);
        return uri.toASCIIString();
    }

    private void rejectNonPublicLiteralAddress(String host) {
        if (!looksLikeIpLiteral(host)) {
            return;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new InvalidRequestException("url", "non-public IP destinations are not allowed");
            }
        } catch (UnknownHostException ex) {
            throw new InvalidRequestException("url", "invalid IP address");
        }
    }

    private boolean looksLikeIpLiteral(String host) {
        return host.indexOf(':') >= 0 || host.chars().allMatch(ch -> Character.isDigit(ch) || ch == '.');
    }
}
