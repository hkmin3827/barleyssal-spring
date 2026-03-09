package com.hakyung.barleyssal_spring.infrastrutrue.kafka.events;

import com.hakyung.barleyssal_spring.domain.order.Order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
    String orderId,
    String accountId,
    String stockCode,
    String orderSide,
    String orderType,
    String quantity,
    BigDecimal limitPrice,
    Instant timestamp
) {
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
                String.valueOf(order.getId()),
                String.valueOf(order.getAccountId()),
                order.getStockCode().value(),
                order.getOrderSide().name(),
                order.getOrderType().name(),
                String.valueOf(order.getQuantity()),
                order.getLimitPrice(),
                Instant.now()
        );
    }
}
