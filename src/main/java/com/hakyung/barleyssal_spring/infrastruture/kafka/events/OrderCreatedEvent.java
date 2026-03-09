package com.hakyung.barleyssal_spring.infrastruture.kafka.events;

import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.shared.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
    Long eventId,
    Instant occurredAt,
    String orderId,
    String accountId,
    String stockCode,
    String orderSide,
    String orderType,
    String quantity,
    BigDecimal limitPrice,
    Instant timestamp
) implements DomainEvent {
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
                order.getId(),
                Instant.now(),
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
    @Override public String aggregateType() { return "ORDER"; }
    @Override public String aggregateId() { return orderId; }
}
