package com.hakyung.barleyssal_spring.infrastruture.kafka.events;

import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.global.utils.TimeConverter;

public record OrderCreatedEvent(
        String orderId,
        String userId,
        String userName,
        Long occurredAt,
        String accountId,
        String stockCode,
        String orderSide,
        String orderType,
        String quantity,
        String limitPrice,
        Long timestamp
) implements DomainEvent {
    public static OrderCreatedEvent from(Order order, Long userId, String userName) {
        long now = TimeConverter.nowMillis();
        return new OrderCreatedEvent(
                String.valueOf(order.getId()),
                String.valueOf(userId),
                userName,
                now,
                String.valueOf(order.getAccountId()),
                order.getStockCode().value(),
                order.getOrderSide().name(),
                order.getOrderType().name(),
                String.valueOf(order.getQuantity()),
                order.getLimitPrice() != null ? order.getLimitPrice().toString() : "",
                now
        );
    }
    @Override public String aggregateType() { return "ORDER"; }
    @Override public String aggregateId() { return orderId; }
}
