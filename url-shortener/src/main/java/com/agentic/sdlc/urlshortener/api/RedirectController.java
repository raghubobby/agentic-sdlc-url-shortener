package com.agentic.sdlc.urlshortener.api;

import com.agentic.sdlc.urlshortener.service.RedirectService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {
    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    @GetMapping("/r/{code}")
    ResponseEntity<Void> redirect(@PathVariable String code,
                                  @RequestHeader(name = "Referer", required = false) String referrer,
                                  @RequestHeader(name = "User-Agent", required = false) String userAgent) {
        var result = redirectService.resolve(code, referrer, userAgent);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(result.destinationUrl()).toASCIIString())
                .header("X-Redirect-Cache", result.cacheHit() ? "HIT" : "MISS")
                .build();
    }
}
