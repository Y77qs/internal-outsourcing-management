package com.pta.outsourcing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

class RedisLockServiceTest {

    @Test
    void shouldAcquireAndReleaseLockWithToken() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("lock-key", "token", Duration.ofSeconds(5))).thenReturn(true);
        RedisLockService service = new RedisLockService(redisTemplate);

        assertThat(service.acquire("lock-key", "token", Duration.ofSeconds(5))).isTrue();
        service.release("lock-key", "token");

        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(java.util.List.of("lock-key")), eq("token"));
    }

    @Test
    void shouldTreatNullAcquireResultAsFailure() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("lock-key", "token", Duration.ofSeconds(5))).thenReturn(null);

        assertThat(new RedisLockService(redisTemplate).acquire("lock-key", "token", Duration.ofSeconds(5))).isFalse();
    }
}
