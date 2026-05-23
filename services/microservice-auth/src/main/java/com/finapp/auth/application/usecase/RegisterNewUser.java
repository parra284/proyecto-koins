package com.finapp.auth.application.usecase;

import com.finapp.auth.application.port.IPasswordHasher;
import com.finapp.auth.application.port.IUserRepository;
import com.finapp.auth.domain.entity.User;
import com.finapp.auth.domain.exception.DuplicateEmailException;
import com.finapp.auth.domain.valueobject.Credentials;

import java.util.Objects;

/**
 * Command Interactor — orchestrates the registration of a new user account.
 *
 * <p>Execution flow:</p>
 * <ol>
 *   <li>Receives raw email + password and constructs a {@link Credentials}
 *       Value Object, which validates RFC 5322 email syntax and banking-grade
 *       password complexity.</li>
 *   <li>Checks email uniqueness via the {@link IUserRepository} port.</li>
 *   <li>Hashes the validated password via the {@link IPasswordHasher} port.</li>
 *   <li>Creates a new {@link User} Aggregate Root with a fresh UUID v4,
 *       default CUSTOMER role, and clean security state.</li>
 *   <li>Persists the user and returns the result.</li>
 * </ol>
 *
 * <p><strong>No Spring annotations.</strong> Wired via constructor injection
 * in the infrastructure configuration.</p>
 */
public class RegisterNewUser {

    private final IUserRepository userRepository;
    private final IPasswordHasher passwordHasher;

    public RegisterNewUser(IUserRepository userRepository,
                           IPasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(
                userRepository, "userRepository must not be null");
        this.passwordHasher = Objects.requireNonNull(
                passwordHasher, "passwordHasher must not be null");
    }

    /**
     * Input command for user registration.
     *
     * @param email       the raw email address
     * @param rawPassword the raw plaintext password
     */
    public record Command(String email, String rawPassword) {
        public Command {
            Objects.requireNonNull(email, "email is required");
            Objects.requireNonNull(rawPassword, "rawPassword is required");
        }
    }

    /**
     * Result carrying the registered user's public identifiers.
     *
     * @param userId the generated opaque UUID
     * @param email  the normalized email
     * @param role   the assigned role name
     */
    public record Result(String userId, String email, String role) {
        public Result {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(email, "email must not be null");
            Objects.requireNonNull(role, "role must not be null");
        }
    }

    /**
     * Executes the registration flow.
     *
     * @param command the registration input
     * @return a {@link Result} with the new user's identifiers
     * @throws com.finapp.auth.domain.exception.InvalidEmailException  if email is malformed
     * @throws com.finapp.auth.domain.exception.WeakPasswordException  if password is weak
     * @throws DuplicateEmailException if email is already registered
     */
    public Result execute(Command command) {

        // 1. Structural validation via the Credentials Value Object
        Credentials credentials = new Credentials(command.email(), command.rawPassword());

        // 2. Uniqueness check
        if (userRepository.existsByEmail(credentials.email())) {
            throw new DuplicateEmailException(credentials.email());
        }

        // 3. Cryptographic hashing (delegated to infrastructure)
        String hashedPassword = passwordHasher.hash(credentials.rawPassword());

        // 4. Aggregate Root creation (UUID v4 + CUSTOMER role + clean state)
        User newUser = User.register(credentials.email(), hashedPassword);

        // 5. Persistence
        User persisted = userRepository.save(newUser);

        return new Result(
                persisted.userId().toString(),
                persisted.email(),
                persisted.role().name()
        );
    }
}
