package com.hakyung.barleyssal_spring.infrastrutrue.kafka.events;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventIdGenerator {
    private final StringRedisTemplate redisTemplate;
    private static final String EVENT_SEQ_KEY = "seq:event:id";

    public Long generate() {
        // Redis에서 값을 1씩 증가시키며 가져옴 (Atomic 연산)
        return redisTemplate.opsForValue().increment(EVENT_SEQ_KEY);
    }
}