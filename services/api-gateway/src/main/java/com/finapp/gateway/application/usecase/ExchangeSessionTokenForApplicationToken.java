package com.finapp.gateway.application.usecase;

import com.finapp.gateway.application.port.IApplicationTokenSigner;
import com.finapp.gateway.application.port.ISessionTokenValidator;
import com.finapp.gateway.domain.valueobject.UserIdentityContext;

import java.util.Objects;

/**
 * Command Interactor — orchestrates the Zero-Trust Identity Bridge flow.
 *
 * <p>This interactor performs the core token exchange:</p>
 * <ol>
 *   <li>Validates the external session token issued by the Authentication service
 *       using the {@link ISessionTokenValidator} port.</li>
 *   <li>Extracts the user identity into a pure domain {@link UserIdentityContext}.</li>
 *   <li>Signs a new, short-lived (1 minute) internal application JWT using the
 *       {@link IApplicationTokenSigner} port, embedding the verified identity.</li>
 * </ol>
 *
 * <p><strong>No Spring annotations.</strong> Wired via constructor injection
 * in the infrastructure configuration.</p>
 */
public class ExchangeSessionTokenForApplicationToken {

    private final ISessionTokenValidator sessionTokenValidator;
    private final IApplicationTokenSigner applicationTokenSigner;

    public ExchangeSessionTokenForApplicationToken(
            ISessionTokenValidator sessionTokenValidator,
            IApplicationTokenSigner applicationTokenSigner) {
        this.sessionTokenValidator = Objects.requireNonNull(
                sessionTokenValidator, "sessionTokenValidator must not be null");
        this.applicationTokenSigner = Objects.requireNonNull(
                applicationTokenSigner, "applicationTokenSigner must not be null");
    }

    /**
     * Result value object carrying both the verified identity and the
     * newly minted internal application token.
     *
     * @param identity         the user identity extracted from the session token
     * @param applicationToken the signed internal JWT for downstream services
     */
    public record Result(UserIdentityContext identity, String applicationToken) {
        public Result {
            Objects.requireNonNull(identity, "identity must not be null");
            Objects.requireNonNull(applicationToken, "applicationToken must not be null");
            if (applicationToken.isBlank()) {
                throw new IllegalArgumentException("applicationToken must not be blank");
            }
        }
    }

    /**
     * Executes the token exchange flow.
     *
     * @param sessionToken the raw JWT from the client's Authorization header
     * @return a {@link Result} containing the verified identity and the new
     *         application token
     * @throws com.finapp.gateway.domain.exception.InvalidSessionTokenException
     *         if the session token is invalid, expired, or tampered with
     */
    public Result execute(String sessionToken) {
        Objects.requireNonNull(sessionToken, "sessionToken must not be null");
        if (sessionToken.isBlank()) {
            throw new IllegalArgumentException("sessionToken must not be blank");
        }

        UserIdentityContext identity = sessionTokenValidator.validate(sessionToken);

        String applicationToken = applicationTokenSigner.sign(identity);

        return new Result(identity, applicationToken);
    }
}
