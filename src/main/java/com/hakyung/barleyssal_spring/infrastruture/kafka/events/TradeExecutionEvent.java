package com.hakyung.barleyssal_spring.infrastruture.kafka.events;

import java.math.BigDecimal;

// 체결 엔진에서 발행하여 통계 서비스가 구독할 이벤트
public record TradeExecutionEvent(
        Long userId,
        String stockCode,
        BigDecimal executedPrice,
        Long quantity,
        Double profitRate, // 체결 시 계산된 수익률
        String orderSide
) {}