package com.pta.outsourcing.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    void shouldGenerateAndParseToken() {
        JwtTokenProvider provider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-bytes-long",
                60
        );

        JwtTokenProvider.JwtToken token = provider.generateToken(10L, "tester");
        Claims claims = provider.parseClaims(token.token());

        assertThat(claims.getSubject()).isEqualTo("10");
        assertThat(claims.get("username", String.class)).isEqualTo("tester");
        assertThat(claims.getId()).isEqualTo(token.jti());
        assertThat(token.expiresIn()).isEqualTo(3600);
    }
}
