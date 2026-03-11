package com.hakyung.barleyssal_spring.infrastruture.kafka.events;

import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.shared.DomainEvent;

import java.time.Instant;

public record OrderCreatedEvent(
        String orderId,
        String userId,
        Instant occurredAt,
        String accountId,
        String stockCode,
        String orderSide,
        String orderType,
        String quantity,
        String limitPrice,
        Instant timestamp
) implements DomainEvent {
    public static OrderCreatedEvent from(Order order, Long userId) {
        return new OrderCreatedEvent(
                String.valueOf(order.getId()),
                String.valueOf(userId),
                Instant.now(),
                String.valueOf(order.getAccountId()),
                order.getStockCode().value(),
                order.getOrderSide().name(),
                order.getOrderType().name(),
                String.valueOf(order.getQuantity()),
                String.valueOf(order.getLimitPrice()),
                Instant.now()
        );
    }
    @Override public String aggregateType() { return "ORDER"; }
    @Override public String aggregateId() { return orderId; }
}
