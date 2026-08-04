package com.challenge.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rejects any request that does not carry the expected API key in its X-API-Key header. */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private final String expectedKey;

    public ApiKeyFilter(@Value("${api.security.api-key}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isValid(request.getHeader(HEADER))) {
            // No body: a rejected caller learns nothing beyond the fact that the key was wrong.
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }

    // Constant-time comparison, so how long the check takes gives nothing away about the real key.
    private boolean isValid(String presentedKey) {
        return presentedKey != null
                && MessageDigest.isEqual(
                        presentedKey.getBytes(StandardCharsets.UTF_8), expectedKey.getBytes(StandardCharsets.UTF_8));
    }
}
