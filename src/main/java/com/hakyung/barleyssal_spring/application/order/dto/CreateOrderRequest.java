package com.hakyung.barleyssal_spring.application.order.dto;

import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.domain.order.OrderType;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
    @NotBlank String stockCode,
    @NotNull OrderSide orderSide,
    @NotNull OrderType orderType,
    @Min(1)   long quantity,
    BigDecimal limitPrice
) {
    public void validateLimitPrice() {
        if (orderType == OrderType.LIMIT && limitPrice == null) {
            throw new CustomException(ErrorCode.LIMIT_PRICE_REQUIRED);
        }
    }
}
