package com.finapp.auth.adapter.input.rest.dto;

/**
 * Immutable DTO — carries the raw authentication credentials from the
 * HTTP boundary into the application layer.
 *
 * <p>Used by both the registration and login endpoints.</p>
 *
 * @param email       the user's email address
 * @param rawPassword the user's plaintext password
 */
public record AuthRequestDTO(String email, String rawPassword) {

    /**
     * Defensive validation — rejects null payloads at the HTTP boundary
     * before they reach the application layer.
     */
    public AuthRequestDTO {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("rawPassword is required");
        }
    }
}
