package com.pta.outsourcing.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperationLogSanitizerTest {

    private OperationLogSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new OperationLogSanitizer(new ObjectMapper());
    }

    @Test
    void shouldMaskLongPasswordBeforeTruncating() {
        String rawPassword = "P".repeat(1500);

        String result = sanitizer.sanitize("{\"username\":\"admin\",\"password\":\"%s\"}".formatted(rawPassword));

        assertThat(result).contains("\"password\":\"******\"");
        assertThat(result).doesNotContain(rawPassword.substring(0, 32));
        assertThat(result).hasSizeLessThanOrEqualTo(1000);
    }

    @Test
    void shouldMaskLongTokenBeforeTruncating() {
        String rawToken = "T".repeat(1500);

        String result = sanitizer.sanitize("{\"accessToken\":\"%s\"}".formatted(rawToken));

        assertThat(result).contains("\"accessToken\":\"******\"");
        assertThat(result).doesNotContain(rawToken.substring(0, 32));
        assertThat(result).hasSizeLessThanOrEqualTo(1000);
    }

    @Test
    void shouldMaskAuthorizationBearerToken() {
        String rawBearer = "Bearer " + "A".repeat(1200);

        String result = sanitizer.sanitize("Authorization=%s, module=auth".formatted(rawBearer));

        assertThat(result).contains("Authorization=******");
        assertThat(result).doesNotContain("Bearer " + "A".repeat(32));
        assertThat(result).hasSizeLessThanOrEqualTo(1000);
    }
}
