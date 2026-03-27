package com.hakyung.barleyssal_spring.global;

import com.hakyung.barleyssal_spring.global.ratelimit.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService 단위 테스트")
class RateLimitServiceTest {

    @InjectMocks
    RateLimitService rateLimitService;
    @Mock
    StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("카운트가 maxRequests 이하이면 허용")
    void allowed_when_count_lte_max() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .willReturn(1L);

        assertThat(rateLimitService.isAllowed(1L, "global", 5, 1)).isTrue();
    }

    @Test
    @DisplayName("카운트가 maxRequests 초과이면 거부")
    void denied_when_count_gt_max() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .willReturn(6L);

        assertThat(rateLimitService.isAllowed(1L, "global", 5, 1)).isFalse();
    }

    @Test
    @DisplayName("Redis null 반환 시 허용 (fail-open)")
    void allowed_when_redis_returns_null() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .willReturn(null);

        assertThat(rateLimitService.isAllowed(1L, "global", 5, 1)).isTrue();
    }

    @Test
    @DisplayName("정확히 maxRequests 횟수면 허용")
    void allowed_exactly_at_max() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .willReturn(2L);

        assertThat(rateLimitService.isAllowed(1L, "/api/v1/orders", 2, 1)).isTrue();
    }

    @Test
    @DisplayName("maxRequests + 1 이면 거부")
    void denied_one_over_max() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .willReturn(3L);

        assertThat(rateLimitService.isAllowed(1L, "/api/v1/orders", 2, 1)).isFalse();
    }
}
