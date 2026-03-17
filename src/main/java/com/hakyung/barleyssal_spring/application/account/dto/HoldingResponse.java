package com.hakyung.barleyssal_spring.application.account.dto;

import com.hakyung.barleyssal_spring.domain.holding.Holding;

import java.math.BigDecimal;

public record HoldingResponse(
    String stockCode,
    long totalQuantity,
    BigDecimal avgPrice
) {
    public static HoldingResponse from(Holding h) {
        return new HoldingResponse(
            h.getStockCode().value(), h.getTotalQuantity(), h.getAvgPrice()
        );
    }
}
