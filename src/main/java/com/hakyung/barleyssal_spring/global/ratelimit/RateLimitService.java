package com.hakyung.barleyssal_spring.global.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "rate:limit:user:";

    public boolean isAllowed(Long userId, String limitScope, int maxRequests, int windowSeconds) {
        String key = KEY_PREFIX + userId + ":" + limitScope;

        String luaScript = "local count = redis.call('INCR', KEYS[1]) "
                       + "if count == 1 then "
                       + "  redis.call('EXPIRE', KEYS[1], ARGV[1]) "
                       + "end "
                       + "return count";

        RedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long count = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(windowSeconds));

        return count != null && count <= maxRequests;
    }
}
