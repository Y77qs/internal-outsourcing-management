package com.pta.outsourcing.security;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginSessionService {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.security.session-prefix}")
    private String sessionPrefix;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    public void save(Long userId, String jti) {
        // Redis 只保存最新一次登录的 jti，实现同一用户退出或重新登录后旧 Token 失效。
        stringRedisTemplate.opsForValue()
                .set(key(userId), jti, Duration.ofMinutes(expirationMinutes));
    }

    public boolean isValid(Long userId, String jti) {
        String cachedJti = stringRedisTemplate.opsForValue().get(key(userId));
        // 同时比较 JWT 内的 jti 和 Redis 缓存值，避免仅凭未过期 Token 绕过退出登录。
        return jti != null && jti.equals(cachedJti);
    }

    public void remove(Long userId) {
        stringRedisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return sessionPrefix + ":" + userId;
    }
}
