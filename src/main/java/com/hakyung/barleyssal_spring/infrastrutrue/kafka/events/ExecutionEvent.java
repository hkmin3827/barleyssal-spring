package com.hakyung.barleyssal_spring.infrastrutrue.kafka.events;

import com.hakyung.barleyssal_spring.domain.order.Order;

import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionEvent(
    String orderId,
    String accountId,
    String stockCode,
    String orderSide,
    String executedQuantity,
    BigDecimal executedPrice,
    Instant timestamp
) {
    public static ExecutionEvent from(Order order) {
        return new ExecutionEvent(
                String.valueOf(order.getId()),
                String.valueOf(order.getAccountId()),
                order.getStockCode().value(),
                order.getOrderSide().name(),
                String.valueOf(order.getExecutedQuantity()),
                order.getExecutedPrice(),
                Instant.now()
        );
    }
}
