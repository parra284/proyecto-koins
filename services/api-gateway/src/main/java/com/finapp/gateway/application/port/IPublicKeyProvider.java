package com.finapp.gateway.application.port;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.PublicKey;

/**
 * Output Port — provides asymmetric public keys from an external
 * authentication authority, isolating the application core from
 * the key retrieval mechanism (static file, JWKS endpoint, vault, etc.).
 *
 * <p>Reactive signatures ensure that key retrieval never blocks the
 * Netty event loop, even when the underlying implementation fetches
 * keys over the network.</p>
 *
 * <p><strong>Framework-free interface</strong> — depends only on
 * Project Reactor types which are the reactive standard for the JVM.</p>
 */
public interface IPublicKeyProvider {

    /**
     * Returns the current <em>primary</em> public key used for
     * session token signature verification.
     *
     * <p>Implementations should serve this from a hot cache to
     * guarantee sub-millisecond latency on the hot path.</p>
     *
     * @return a {@link Mono} emitting the current public key,
     *         or an error signal if no key is available
     */
    Mono<PublicKey> getPublicKey();

    /**
     * Returns all currently known public keys (e.g., during key rotation
     * the previous key may still be valid for in-flight tokens).
     *
     * @return a {@link Flux} emitting all available public keys
     */
    Flux<PublicKey> getAllPublicKeys();
}
