package com.hakyung.barleyssal_spring.infrastrutrue.redis;

import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.domain.order.OrderStatus;
import com.hakyung.barleyssal_spring.domain.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisOrderRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "orders:pending:";

    // 주문 저장 (ZSET: Score는 가격, Value는 주문ID)
    public void savePendingOrder(OrderSide side, OrderType type, String stockCode, Long orderId, BigDecimal limitPrice) {
        String key = KEY_PREFIX + stockCode;
        // ZSET에 추가: ZADD orders:pending:005930 70000 "123"
        String value = String.format("%d:%s:%s",
                orderId,
                side,
                type);

        redisTemplate.opsForZSet().add(key, value, limitPrice.doubleValue());
    }
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

    // 체결 대상 조회 (현재가와 일치하는 주문들 찾기)
    public Set<String> findMatchableOrders(String stockCode, BigDecimal currentPrice) {
        String key = KEY_PREFIX + stockCode;
        // 특정 가격 범위의 주문 ID들 조회: ZRANGEBYSCORE key price price
        return redisTemplate.opsForZSet().rangeByScore(key, currentPrice.doubleValue(), currentPrice.doubleValue());
    }
    public Set<String> findMatchableOrders(String stockCode, OrderSide side, BigDecimal currentPrice) {
        String key = String.format("%s%s:%s", KEY_PREFIX, stockCode, side);
        double price = currentPrice.doubleValue();

        if (side == OrderSide.BUY) {
            // 매수: 내가 사려는 가격(limitPrice) >= 현재가 인 주문들 모두 추출
            // ZRANGEBYSCORE key currentPrice +inf
            return redisTemplate.opsForZSet().rangeByScore(key, price, Double.MAX_VALUE);
        } else {
            // 매도: 내가 팔려는 가격(limitPrice) <= 현재가 인 주문들 모두 추출
            // ZRANGEBYSCORE key -inf currentPrice
            return redisTemplate.opsForZSet().rangeByScore(key, 0, price);
        }
    }

    // 체결/취소 시 삭제
    public void removeOrder(String stockCode, Long orderId) {
        String key = KEY_PREFIX + stockCode;
        redisTemplate.opsForZSet().remove(key, String.valueOf(orderId));
    }
}