package com.finapp.auth.application.usecase;

import com.finapp.auth.application.port.IPasswordHasher;
import com.finapp.auth.application.port.ISessionTokenSigner;
import com.finapp.auth.application.port.IUserRepository;
import com.finapp.auth.domain.entity.User;
import com.finapp.auth.domain.exception.AccountLockedException;
import com.finapp.auth.domain.exception.InvalidCredentialsException;
import com.finapp.auth.domain.valueobject.AccountSecurityState;

import java.util.Objects;

/**
 * Command Interactor — orchestrates the defensive authentication flow
 * with brute-force lockout protection.
 *
 * <p><strong>Defensive flow (executed in strict order):</strong></p>
 * <ol>
 *   <li><strong>User lookup:</strong> Retrieves the user by email. If not found,
 *       throws {@link InvalidCredentialsException} (anti-enumeration: same message
 *       for missing email and wrong password).</li>
 *   <li><strong>Lockout evaluation:</strong> Inspects {@link AccountSecurityState}
 *       in domain memory. If the account is locked (failed attempts ≥ threshold
 *       AND lockout has not expired), immediately throws
 *       {@link AccountLockedException}.</li>
 *   <li><strong>Password verification:</strong> Delegates to the
 *       {@link IPasswordHasher} port for constant-time hash comparison.</li>
 *   <li><strong>On failure:</strong>
 *       <ul>
 *         <li>Increments the failed-attempt counter via
 *             {@link AccountSecurityState#recordFailedAttempt()}.</li>
 *         <li>If the new count reaches {@link AccountSecurityState#MAX_FAILED_ATTEMPTS},
 *             invokes {@link LockUserAccount} to apply an immediate persistent lockout.</li>
 *         <li>Persists the updated user aggregate.</li>
 *         <li>Throws {@link InvalidCredentialsException}.</li>
 *       </ul>
 *   </li>
 *   <li><strong>On success:</strong>
 *       <ul>
 *         <li>Resets the failed-attempt counter via
 *             {@link AccountSecurityState#resetAttempts()}.</li>
 *         <li>Persists the updated user aggregate.</li>
 *         <li>Signs and returns a session JWT via the
 *             {@link ISessionTokenSigner} port.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p><strong>No Spring annotations.</strong> Wired via constructor injection
 * in the infrastructure configuration.</p>
 */
public class AuthenticateUser {

    private final IUserRepository userRepository;
    private final IPasswordHasher passwordHasher;
    private final ISessionTokenSigner sessionTokenSigner;
    private final LockUserAccount lockUserAccount;

    public AuthenticateUser(IUserRepository userRepository,
                            IPasswordHasher passwordHasher,
                            ISessionTokenSigner sessionTokenSigner,
                            LockUserAccount lockUserAccount) {
        this.userRepository = Objects.requireNonNull(
                userRepository, "userRepository must not be null");
        this.passwordHasher = Objects.requireNonNull(
                passwordHasher, "passwordHasher must not be null");
        this.sessionTokenSigner = Objects.requireNonNull(
                sessionTokenSigner, "sessionTokenSigner must not be null");
        this.lockUserAccount = Objects.requireNonNull(
                lockUserAccount, "lockUserAccount must not be null");
    }

    /**
     * Input command carrying the login credentials.
     *
     * @param email       the user's email address
     * @param rawPassword the user's plaintext password
     */
    public record Command(String email, String rawPassword) {
        public Command {
            Objects.requireNonNull(email, "email is required");
            Objects.requireNonNull(rawPassword, "rawPassword is required");
        }
    }

    /**
     * Result carrying the session token and the authenticated user's
     * public identifiers.
     *
     * @param token  the signed session JWT
     * @param userId the user's opaque UUID
     * @param email  the user's email
     * @param role   the user's role name
     */
    public record Result(String token, String userId, String email, String role) {
        public Result {
            Objects.requireNonNull(token, "token must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(email, "email must not be null");
            Objects.requireNonNull(role, "role must not be null");
        }
    }

    /**
     * Executes the defensive authentication flow.
     *
     * @param command the login credentials
     * @return a {@link Result} containing the session token and user info
     * @throws InvalidCredentialsException if the email is not found or the
     *                                     password is incorrect
     * @throws AccountLockedException      if the account is currently locked
     */
    public Result execute(Command command) {
        String email = command.email().trim().toLowerCase();

        // ── 1. User lookup (anti-enumeration) ────────────────
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        // ── 2. Lockout evaluation ────────────────────────────
        if (user.securityState().isLocked()) {
            throw new AccountLockedException(email);
        }

        // ── 3. Password verification ─────────────────────────
        boolean passwordValid = passwordHasher.verify(
                command.rawPassword(), user.passwordHash());

        if (!passwordValid) {
            return handleFailedAttempt(user);
        }

        // ── 4. Successful authentication ─────────────────────
        return handleSuccessfulLogin(user);
    }

    /* ────────────────────────────────────────────────────────
       Private — failure path
       ──────────────────────────────────────────────────────── */

    /**
     * Handles a failed login attempt: increments the counter, triggers
     * lockout if threshold reached, persists state, and throws.
     *
     * @return never returns — always throws
     * @throws InvalidCredentialsException always
     */
    private Result handleFailedAttempt(User user) {
        AccountSecurityState updatedState = user.securityState().recordFailedAttempt();
        User updatedUser = user.withSecurityState(updatedState);

        if (updatedState.hasExceededMaxAttempts()) {
            // Delegate lockout to the dedicated interactor (persists internally)
            lockUserAccount.execute(user.userId());
        } else {
            // Persist the incremented attempt counter
            userRepository.save(updatedUser);
        }

        throw new InvalidCredentialsException();
    }

    /* ────────────────────────────────────────────────────────
       Private — success path
       ──────────────────────────────────────────────────────── */

    /**
     * Handles a successful login: resets attempts, persists state,
     * and signs the session token.
     */
    private Result handleSuccessfulLogin(User user) {
        // Reset failed attempts
        AccountSecurityState cleanState = user.securityState().resetAttempts();
        User cleanUser = user.withSecurityState(cleanState);
        userRepository.save(cleanUser);

        // Sign session JWT
        String token = sessionTokenSigner.sign(
                cleanUser.userId().toString(),
                cleanUser.email(),
                cleanUser.role().name()
        );

        return new Result(
                token,
                cleanUser.userId().toString(),
                cleanUser.email(),
                cleanUser.role().name()
        );
    }
}
