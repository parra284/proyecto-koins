package com.finapp.auth.adapter.input.rest;

import com.finapp.auth.adapter.input.rest.dto.AuthRequestDTO;
import com.finapp.auth.adapter.input.rest.dto.AuthResponseDTO;
import com.finapp.auth.application.usecase.AuthenticateUser;
import com.finapp.auth.application.usecase.RegisterNewUser;
import com.finapp.auth.application.usecase.RevokeAccess;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perimeter REST Controller — exposes the authentication endpoints
 * for user registration, login, and token revocation.
 *
 * <p>This controller is the HTTP boundary adapter. It translates incoming
 * HTTP requests into application-layer commands and maps results back to
 * DTOs. All business logic is delegated to framework-free interactors.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterNewUser registerNewUser;
    private final AuthenticateUser authenticateUser;
    private final RevokeAccess revokeAccess;

    public AuthController(RegisterNewUser registerNewUser,
                          AuthenticateUser authenticateUser,
                          RevokeAccess revokeAccess) {
        this.registerNewUser = registerNewUser;
        this.authenticateUser = authenticateUser;
        this.revokeAccess = revokeAccess;
    }

    /**
     * POST /api/v1/auth/register — creates a new user account.
     *
     * @param request the registration credentials
     * @return HTTP 201 with the new user's identifiers
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody AuthRequestDTO request) {
        RegisterNewUser.Command command = new RegisterNewUser.Command(
                request.email(), request.rawPassword());

        RegisterNewUser.Result result = registerNewUser.execute(command);

        AuthResponseDTO response = AuthResponseDTO.ofRegistration(
                result.userId(), result.email(), result.role());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/auth/login — authenticates a user and returns a session JWT.
     *
     * @param request the login credentials
     * @return HTTP 200 with the session token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        AuthenticateUser.Command command = new AuthenticateUser.Command(
                request.email(), request.rawPassword());

        AuthenticateUser.Result result = authenticateUser.execute(command);

        AuthResponseDTO response = AuthResponseDTO.ofLogin(
                result.token(), result.userId(), result.email(), result.role());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/auth/revoke — revokes an active session token.
     *
     * @param tokenId    the JTI of the token to revoke (from header)
     * @param ttlSeconds the remaining TTL in seconds (from header)
     * @return HTTP 204 No Content
     */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(
            @RequestHeader("X-Token-Id") String tokenId,
            @RequestHeader("X-Token-TTL") long ttlSeconds) {

        RevokeAccess.Command command = new RevokeAccess.Command(tokenId, ttlSeconds);
        revokeAccess.execute(command);

        return ResponseEntity.noContent().build();
    }
}
