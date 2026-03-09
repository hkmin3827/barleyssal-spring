package com.hakyung.barleyssal_spring.domain.account;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {
    private final StringRedisTemplate redisTemplate;
    private static final String SERVICE_CODE = "10";
    private static final String REDIS_KEY_PREFIX = "acc_seq:";

    public String generate() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String key = REDIS_KEY_PREFIX + today;

        Long sequence = redisTemplate.opsForValue().increment(key);

        if (sequence != null && sequence == 1L) {
            redisTemplate.expire(key, 25, TimeUnit.HOURS);
        }

        String sequencePart = String.format("%06d", sequence);

        return String.format("%s-%s-%s", today, SERVICE_CODE, sequencePart);
    }
}