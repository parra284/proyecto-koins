package com.finapp.auth.application.usecase;

import com.finapp.auth.application.port.IUserRepository;
import com.finapp.auth.domain.entity.User;
import com.finapp.auth.domain.valueobject.AccountSecurityState;

import java.util.Objects;
import java.util.UUID;

/**
 * Internal Command Interactor — immediately locks a user account that
 * has exceeded the maximum allowed failed login attempts.
 *
 * <p>This interactor is invoked by {@link AuthenticateUser} when the
 * brute-force threshold is breached. It performs an immediate, persistent
 * lockout by:</p>
 * <ol>
 *   <li>Retrieving the current user state.</li>
 *   <li>Applying the {@link AccountSecurityState#lock()} transition, which sets
 *       the lockout expiration to {@code now + LOCKOUT_DURATION}.</li>
 *   <li>Persisting the updated aggregate immediately.</li>
 * </ol>
 *
 * <p><strong>No Spring annotations.</strong> Wired via constructor injection
 * in the infrastructure configuration.</p>
 */
public class LockUserAccount {

    private final IUserRepository userRepository;

    public LockUserAccount(IUserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(
                userRepository, "userRepository must not be null");
    }

    /**
     * Locks the account identified by the given userId.
     *
     * @param userId the UUID of the account to lock
     * @throws IllegalArgumentException if the user is not found
     */
    public void execute(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot lock account: user not found for id " + userId));

        AccountSecurityState lockedState = user.securityState().lock();
        User lockedUser = user.withSecurityState(lockedState);

        userRepository.save(lockedUser);
    }
}
