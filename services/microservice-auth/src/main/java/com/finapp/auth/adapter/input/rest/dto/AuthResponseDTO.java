package com.finapp.auth.adapter.input.rest.dto;

import java.time.Instant;

/**
 * Immutable DTO — carries the authentication result from the application
 * layer back to the HTTP client.
 *
 * @param token     the signed session JWT (present only on login success)
 * @param userId    the user's opaque UUID
 * @param email     the user's normalized email
 * @param role      the user's authorization role
 * @param issuedAt  the timestamp when the response was generated
 */
public record AuthResponseDTO(
        String token,
        String userId,
        String email,
        String role,
        Instant issuedAt
) {
    /**
     * Factory for login responses (includes token).
     */
    public static AuthResponseDTO ofLogin(String token, String userId,
                                          String email, String role) {
        return new AuthResponseDTO(token, userId, email, role, Instant.now());
    }

    /**
     * Factory for registration responses (no token).
     */
    public static AuthResponseDTO ofRegistration(String userId, String email,
                                                  String role) {
        return new AuthResponseDTO(null, userId, email, role, Instant.now());
    }
}
