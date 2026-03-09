package com.hakyung.barleyssal_spring.infrastrutrue.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "rate:limit:user:";

    public boolean isAllowed(Long userId, int maxRequests, int windowSeconds) {
        String key = KEY_PREFIX + userId;

        Long count = redisTemplate.opsForValue().increment(key);
        if(count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        return count != null && count <= maxRequests;
    }

}
