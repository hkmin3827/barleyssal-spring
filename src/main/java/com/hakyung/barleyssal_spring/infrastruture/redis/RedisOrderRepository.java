package com.hakyung.barleyssal_spring.infrastruture.redis;

import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.domain.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RedisOrderRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String ZSET_KEY_PREFIX = "orders:pending:";
    private static final String HASH_KEY_PREFIX = "order:meta:";

    public void saveLimitOrder(Order order, Long userId) {
        if (order.getOrderType() == OrderType.MARKET) return;

        String zsetKey = String.format("%s%s:%s", ZSET_KEY_PREFIX, order.getStockCode().value(), order.getOrderSide());
        String hashKey = HASH_KEY_PREFIX + order.getId();

        redisTemplate.opsForZSet().add(zsetKey,
                String.valueOf(order.getId()),
                order.getLimitPrice().doubleValue());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", String.valueOf(order.getId()));
        metadata.put("userId", String.valueOf(userId));
        metadata.put("accountId", String.valueOf(order.getAccountId()));
        metadata.put("stockCode", order.getStockCode().value());
        metadata.put("quantity", String.valueOf(order.getQuantity()));
        metadata.put("orderSide", order.getOrderSide().name());
        metadata.put("limitPrice", order.getLimitPrice().toString());

        redisTemplate.opsForHash().putAll(hashKey, metadata);

        redisTemplate.expire(hashKey, Duration.ofDays(1));
    }

    public void removeLimitOrder(Long orderId, StockCode stockCode, OrderSide orderSide) {
        String key = String.format("%s%s:%s",
                ZSET_KEY_PREFIX,
                stockCode.value(),
                orderSide
        );
        String hashKey = HASH_KEY_PREFIX + orderId;

        redisTemplate.opsForZSet().remove(key, String.valueOf(orderId));
        redisTemplate.delete(hashKey);
    }
}