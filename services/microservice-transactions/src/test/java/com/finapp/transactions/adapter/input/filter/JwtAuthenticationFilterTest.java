package com.finapp.transactions.adapter.input.filter;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthenticationFilterTest {

    @Test
    void validTokenInjectsUserIdAndContinuesChain() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(() -> keyPair.getPublic());
        MockHttpServletRequest request = requestWithToken(signedToken(keyPair, "user-123"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute("userId")).isEqualTo("user-123");
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(() -> keyPair.getPublic());
        MockHttpServletRequest request = requestWithToken("not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or expired JWT token");
    }

    @Test
    void downstreamExceptionsAreNotReportedAsJwtFailures() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(() -> keyPair.getPublic());
        MockHttpServletRequest request = requestWithToken(signedToken(keyPair, "user-123"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, throwingChain()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    private static FilterChain throwingChain() {
        return new FilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request,
                    jakarta.servlet.ServletResponse response) throws IOException, ServletException {
                throw new IllegalStateException("boom");
            }
        };
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transactions");
        request.addHeader("X-Internal-Token", "Bearer " + token);
        return request;
    }

    private static String signedToken(KeyPair keyPair, String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
