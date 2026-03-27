package com.hakyung.barleyssal_spring.infrastruture.redis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class RedisConnectionTest {

    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void test() {
        redisTemplate.opsForValue().set("hello", "redis");
        String value = redisTemplate.opsForValue().get("hello");
        System.out.println("REDIS TEST VALUE = " + value);
    }
}