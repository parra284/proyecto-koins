package com.finapp.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository — provides CRUD and custom queries for
 * the {@link UserDbModel} entity.
 *
 * <p>This interface is consumed exclusively by the
 * {@link com.finapp.auth.adapter.output.persistence.RelationalUserRepositoryAdapter},
 * never by application or domain code.</p>
 */
@Repository
public interface JpaUserRepository extends JpaRepository<UserDbModel, UUID> {

    /**
     * Finds a user by email (case-insensitive search).
     */
    Optional<UserDbModel> findByEmailIgnoreCase(String email);

    /**
     * Checks whether a user with the given email exists (case-insensitive).
     */
    boolean existsByEmailIgnoreCase(String email);
}
