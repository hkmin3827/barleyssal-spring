package com.hakyung.barleyssal_spring.application;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisConnectionTest {

    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void test() {
        redisTemplate.opsForValue().set("hello", "redis");
        String value = redisTemplate.opsForValue().get("hello");
        System.out.println("REDIS TEST VALUE = " + value);
    }
}