package com.finapp.auth.application.usecase;

import com.finapp.auth.application.port.IDenylistRepository;

import java.util.Objects;

/**
 * Command Interactor — revokes an active session token by registering
 * its identifier (JTI) in the high-speed denylist.
 *
 * <p>Once revoked, the API Gateway's token validation will reject the
 * token even if it has not yet expired cryptographically.</p>
 *
 * <p><strong>No Spring annotations.</strong> Wired via constructor injection
 * in the infrastructure configuration.</p>
 */
public class RevokeAccess {

    private final IDenylistRepository denylistRepository;

    public RevokeAccess(IDenylistRepository denylistRepository) {
        this.denylistRepository = Objects.requireNonNull(
                denylistRepository, "denylistRepository must not be null");
    }

    /**
     * Input command for token revocation.
     *
     * @param tokenId    the unique identifier (JTI) of the token to revoke
     * @param ttlSeconds the remaining validity of the token in seconds
     *                   (the denylist entry auto-expires after this period)
     */
    public record Command(String tokenId, long ttlSeconds) {
        public Command {
            Objects.requireNonNull(tokenId, "tokenId is required");
            if (tokenId.isBlank()) {
                throw new IllegalArgumentException("tokenId must not be blank");
            }
            if (ttlSeconds <= 0) {
                throw new IllegalArgumentException(
                        "ttlSeconds must be positive, got: " + ttlSeconds);
            }
        }
    }

    /**
     * Executes the token revocation.
     *
     * @param command the revocation input
     */
    public void execute(Command command) {
        denylistRepository.revoke(command.tokenId(), command.ttlSeconds());
    }
}
