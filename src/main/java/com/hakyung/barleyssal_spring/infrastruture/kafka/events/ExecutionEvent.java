package com.hakyung.barleyssal_spring.infrastruture.kafka.events;

import com.hakyung.barleyssal_spring.domain.order.Order;

import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionEvent(
    String orderId,
    String userId,
    String userName,
    String accountId,
    String stockCode,
    String orderSide,
    String orderType,
    String executedQuantity,
    BigDecimal executedPrice,
    Long timestamp,
    String executionStatus
) {}
