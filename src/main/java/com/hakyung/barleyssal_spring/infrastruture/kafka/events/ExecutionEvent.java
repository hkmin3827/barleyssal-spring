package com.hakyung.barleyssal_spring.infrastruture.kafka.events;

import com.hakyung.barleyssal_spring.domain.order.Order;

import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionEvent(
    String orderId,
    String userId,
    String accountId,
    String stockCode,
    String orderSide,
    String executedQuantity,
    BigDecimal executedPrice,
    Instant timestamp
) {
    public static ExecutionEvent from(Order order, Long userId) {
        return new ExecutionEvent(
                String.valueOf(order.getId()),
                String.valueOf(userId),
                String.valueOf(order.getAccountId()),
                order.getStockCode().value(),
                order.getOrderSide().name(),
                String.valueOf(order.getExecutedQuantity()),
                order.getExecutedPrice(),
                Instant.now()
        );
    }

    public ExecutionEvent(String orderId, String userId, String accountId, String stockCode,
                          String orderSide, String qty, BigDecimal price) {
        this(orderId, userId, accountId,
                stockCode, orderSide, qty, price, Instant.now());
    }
}
