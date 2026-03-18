package com.hakyung.barleyssal_spring.infrastruture.redis;

import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.domain.order.OrderType;
import com.hakyung.barleyssal_spring.infrastruture.kafka.events.ExecutionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisOrderRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String ZSET_KEY_PREFIX = "orders:pending:";
    private static final String HASH_KEY_PREFIX = "order:meta:";

    public void saveLimitOrder(Order order, Long userId, String userName) {
        if (order.getOrderType() == OrderType.MARKET) return;

        String zsetKey = String.format("%s%s:%s", ZSET_KEY_PREFIX, order.getStockCode().value(), order.getOrderSide());
        String hashKey = HASH_KEY_PREFIX + order.getId();

        redisTemplate.opsForZSet().add(zsetKey,
                String.valueOf(order.getId()),
                order.getLimitPrice().doubleValue());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", String.valueOf(order.getId()));
        metadata.put("userId", String.valueOf(userId));
        metadata.put("userName", String.valueOf(userName));
        metadata.put("accountId", String.valueOf(order.getAccountId()));
        metadata.put("stockCode", order.getStockCode().value());
        metadata.put("quantity", String.valueOf(order.getQuantity()));
        metadata.put("orderSide", order.getOrderSide().name());
        metadata.put("orderType", order.getOrderType().name());
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

    public void rollbackOrderToRedis(ExecutionEvent event) {
        try {
            String hashKey = "order:meta:" + event.orderId();
            Object limitPriceObj = redisTemplate.opsForHash().get(hashKey, "limitPrice");

            if (limitPriceObj != null) {
                String zsetKey = String.format("orders:pending:%s:%s", event.stockCode(), event.orderSide());
                double limitPrice = Double.parseDouble(limitPriceObj.toString());

                redisTemplate.opsForZSet().add(zsetKey, event.orderId(), limitPrice);
                log.info("Order rolled back to Redis ZSET: orderId={}", event.orderId());
            } else {
                log.warn("Cannot rollback. Hash metadata not found for orderId={}", event.orderId());
            }
        } catch (Exception ex) {
            log.error("Failed to rollback order to Redis: orderId={}", event.orderId(), ex);
        }
    }
}