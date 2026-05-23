package com.finapp.auth.adapter.output.persistence;

import com.finapp.auth.application.port.IUserRepository;
import com.finapp.auth.domain.entity.User;
import com.finapp.auth.domain.valueobject.AccountSecurityState;
import com.finapp.auth.domain.valueobject.Role;
import com.finapp.auth.infrastructure.persistence.JpaUserRepository;
import com.finapp.auth.infrastructure.persistence.UserDbModel;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter — implements {@link IUserRepository} by mapping between the
 * domain {@link User} aggregate and the JPA {@link UserDbModel} entity.
 *
 * <p>This is the anti-corruption layer that isolates the pure domain from
 * ORM concerns. The JPA entity ({@code UserDbModel}) carries Hibernate
 * annotations and lives in the infrastructure layer; this adapter
 * translates bidirectionally.</p>
 */
public class RelationalUserRepositoryAdapter implements IUserRepository {

    private final JpaUserRepository jpaRepository;

    public RelationalUserRepositoryAdapter(JpaUserRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(
                jpaRepository, "jpaRepository must not be null");
    }

    @Override
    public User save(User user) {
        UserDbModel model = toDbModel(user);
        UserDbModel saved = jpaRepository.save(model);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmailIgnoreCase(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmailIgnoreCase(email);
    }

    /* ────────────────────────────────────────────────────────
       Mapping: Domain → JPA
       ──────────────────────────────────────────────────────── */

    private UserDbModel toDbModel(User user) {
        UserDbModel model = new UserDbModel();
        model.setUserId(user.userId());
        model.setEmail(user.email());
        model.setPasswordHash(user.passwordHash());
        model.setRole(user.role().name());
        model.setFailedLoginAttempts(user.securityState().failedLoginAttempts());
        model.setLockoutExpiration(user.securityState().lockoutExpiration());
        return model;
    }

    /* ────────────────────────────────────────────────────────
       Mapping: JPA → Domain
       ──────────────────────────────────────────────────────── */

    private User toDomain(UserDbModel model) {
        AccountSecurityState securityState = new AccountSecurityState(
                model.getFailedLoginAttempts(),
                model.getLockoutExpiration()
        );

        return new User(
                model.getUserId(),
                model.getEmail(),
                model.getPasswordHash(),
                Role.fromString(model.getRole()),
                securityState
        );
    }
}
