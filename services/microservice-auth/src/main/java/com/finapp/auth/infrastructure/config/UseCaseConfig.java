package com.finapp.auth.infrastructure.config;

import com.finapp.auth.adapter.input.presenter.SecurityAuditPresenter;
import com.finapp.auth.adapter.output.persistence.RelationalUserRepositoryAdapter;
import com.finapp.auth.application.port.IDenylistRepository;
import com.finapp.auth.application.port.IKeyProvider;
import com.finapp.auth.application.port.IPasswordHasher;
import com.finapp.auth.application.port.ISessionTokenSigner;
import com.finapp.auth.application.port.IUserRepository;
import com.finapp.auth.application.usecase.AuthenticateUser;
import com.finapp.auth.application.usecase.LockUserAccount;
import com.finapp.auth.application.usecase.RegisterNewUser;
import com.finapp.auth.application.usecase.RevokeAccess;
import com.finapp.auth.infrastructure.cache.FailedLoginCacheAdapter;
import com.finapp.auth.infrastructure.persistence.JpaUserRepository;
import com.finapp.auth.infrastructure.security.BCryptPasswordHasherAdapter;
import com.finapp.auth.infrastructure.security.JwtSessionTokenSignerAdapter;
import com.finapp.auth.infrastructure.security.RsaKeypairGeneratorAdapter;
import com.finapp.auth.infrastructure.security.SecretsVaultAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Spring configuration — wires application-layer interactors, port
 * implementations, and adapter beans via constructor injection,
 * bridging the Dependency Inversion boundary.
 *
 * <p>
 * The interactors and domain classes are pure Java with no Spring
 * annotations. This configuration is the single point where Spring's DI
 * container supplies the infrastructure implementations of the ports.
 * </p>
 */
@Configuration
public class UseCaseConfig {

    private static final Logger log = LoggerFactory.getLogger(UseCaseConfig.class);

    /*
     * ────────────────────────────────────────────────────────
     * Port Implementations (Adapters)
     * ────────────────────────────────────────────────────────
     */

    @Bean
    public IUserRepository userRepository(JpaUserRepository jpaUserRepository) {
        return new RelationalUserRepositoryAdapter(jpaUserRepository);
    }

    @Bean
    public IPasswordHasher passwordHasher() {
        return new BCryptPasswordHasherAdapter();
    }

    @Bean
    public IKeyProvider keyProvider(
            @Value("${auth.security.jwt-public-key:}") String jwtPublicKey,
            @Value("${auth.security.jwt-private-key:}") String jwtPrivateKey,
            @Value("${auth.security.jwt-key-id:}") String jwtKeyId) {

        boolean keysProvided = jwtPublicKey != null && !jwtPublicKey.isBlank()
                && jwtPrivateKey != null && !jwtPrivateKey.isBlank();

        if (keysProvided) {
            log.info("JWT keys detected in environment — loading via SecretsVaultAdapter");
            SecretsVaultAdapter vault = new SecretsVaultAdapter();
            String kid = (jwtKeyId != null && !jwtKeyId.isBlank())
                    ? jwtKeyId
                    : java.util.UUID.randomUUID().toString();
            return vault.loadKeyPairFromPem(jwtPublicKey, jwtPrivateKey, kid);
        }

        log.warn("No JWT keys found in environment — generating ephemeral RSA key pair. "
                + "Tokens will NOT survive restarts.");
        return new RsaKeypairGeneratorAdapter();
    }

    @Bean
    public ISessionTokenSigner sessionTokenSigner(IKeyProvider keyProvider) {
        return new JwtSessionTokenSignerAdapter(keyProvider);
    }

    @Bean
    public IDenylistRepository denylistRepository(StringRedisTemplate redisTemplate) {
        return new FailedLoginCacheAdapter(redisTemplate);
    }

    /*
     * ────────────────────────────────────────────────────────
     * Infrastructure Components
     * ────────────────────────────────────────────────────────
     */

    @Bean
    public SecurityAuditPresenter securityAuditPresenter() {
        return new SecurityAuditPresenter();
    }

    /*
     * ────────────────────────────────────────────────────────
     * Interactors (Use Cases)
     * ────────────────────────────────────────────────────────
     */

    @Bean
    public LockUserAccount lockUserAccount(IUserRepository userRepository) {
        return new LockUserAccount(userRepository);
    }

    @Bean
    public RegisterNewUser registerNewUser(IUserRepository userRepository,
            IPasswordHasher passwordHasher) {
        return new RegisterNewUser(userRepository, passwordHasher);
    }

    @Bean
    public AuthenticateUser authenticateUser(IUserRepository userRepository,
            IPasswordHasher passwordHasher,
            ISessionTokenSigner sessionTokenSigner,
            LockUserAccount lockUserAccount) {
        return new AuthenticateUser(
                userRepository, passwordHasher, sessionTokenSigner, lockUserAccount);
    }

    @Bean
    public RevokeAccess revokeAccess(IDenylistRepository denylistRepository) {
        return new RevokeAccess(denylistRepository);
    }
}
