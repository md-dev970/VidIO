package com.mddev.videoservice.security;

import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(
        String id,
        String username,
        String email
) {
    public static AuthenticatedUser from(Jwt jwt) {
        String username = firstNonBlank(jwt.getClaimAsString("preferred_username"), jwt.getSubject());
        return new AuthenticatedUser(
                jwt.getSubject(),
                username,
                jwt.getClaimAsString("email")
        );
    }

    private static String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }
}
