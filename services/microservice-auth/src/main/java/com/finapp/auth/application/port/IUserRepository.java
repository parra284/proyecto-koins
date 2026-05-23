package com.finapp.auth.application.port;

import com.finapp.auth.domain.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Output Port — abstracts the persistence of {@link User} aggregates.
 *
 * <p>Implementations reside in the adapter/infrastructure layer and may
 * use JPA, JDBC, or any other persistence technology.</p>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface IUserRepository {

    /**
     * Persists a new or updated user aggregate.
     *
     * @param user the user to save
     * @return the persisted user (may include generated metadata)
     */
    User save(User user);

    /**
     * Finds a user by their opaque UUID.
     *
     * @param userId the user's unique identifier
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findById(UUID userId);

    /**
     * Finds a user by their email address (case-insensitive).
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether an account with the given email already exists.
     *
     * @param email the email to check
     * @return {@code true} if the email is already registered
     */
    boolean existsByEmail(String email);
}
