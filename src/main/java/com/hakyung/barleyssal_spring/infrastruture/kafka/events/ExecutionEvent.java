package com.hakyung.barleyssal_spring.infrastruture.kafka.events;

import java.math.BigDecimal;

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
