package com.finapp.gateway.application.port;

import com.finapp.gateway.domain.entity.RouteMapping;

import java.util.List;
import java.util.Optional;

/**
 * Output Port — provides read access to the catalogue of
 * {@link RouteMapping} entries configured for the gateway.
 *
 * <p>Implementations may read from YAML configuration, a database,
 * or an in-memory registry.</p>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface IRouteMappingRepository {

    /**
     * Finds the first {@link RouteMapping} whose
     * {@link RouteMapping#matches(String) pattern matches} the given path.
     *
     * @param requestPath the incoming HTTP request path
     * @return an {@link Optional} containing the matching route, or empty
     *         if no route is found
     */
    Optional<RouteMapping> findByPath(String requestPath);

    /**
     * Returns all registered route mappings.
     *
     * @return an unmodifiable list of all configured routes
     */
    List<RouteMapping> findAll();
}
