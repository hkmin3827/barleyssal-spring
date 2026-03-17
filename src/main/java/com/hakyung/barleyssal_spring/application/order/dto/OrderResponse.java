package com.hakyung.barleyssal_spring.application.order.dto;

import com.hakyung.barleyssal_spring.domain.order.*;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    Long id,
    Long accountId,
    String stockCode,
    OrderSide orderSide,
    OrderType orderType,
    OrderStatus orderStatus,
    String orderRejectReason,
    long quantity,
    BigDecimal limitPrice,
    long executedQuantity,
    BigDecimal executedPrice,
    Instant createdAt,
    Instant updatedAt
) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(
            o.getId(), o.getAccountId(), String.valueOf(o.getStockCode()),
            o.getOrderSide(), o.getOrderType(), o.getOrderStatus(), o.getRejectReason() != null ? o.getRejectReason().getDescription() : null,
            o.getQuantity(),   o.getLimitPrice(), o.getExecutedQuantity(),
            o.getExecutedPrice(), o.getCreatedAt(), o.getUpdatedAt()
        );
    }
}
