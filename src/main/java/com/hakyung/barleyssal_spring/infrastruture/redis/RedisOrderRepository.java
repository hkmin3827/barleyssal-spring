package com.hakyung.barleyssal_spring.infrastruture.redis;

import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisOrderRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "orders:pending:";

    public void saveLimitOrder(Order order) {
        if (order.getOrderType() == OrderType.MARKET) return;

        String key = String.format("%s%s:%s",
                KEY_PREFIX,
                order.getStockCode(),
                order.getOrderSide());

        redisTemplate.opsForZSet().add(key,
                String.valueOf(order.getId()),
                order.getLimitPrice().doubleValue());
    }

    public void removeLimitOrder(Order order) {
        String key = String.format("%s%s:%s",
                KEY_PREFIX,
                order.getStockCode().value(),
                order.getOrderSide());

        redisTemplate.opsForZSet().remove(key, String.valueOf(order.getId()));
    }
}