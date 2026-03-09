package com.hakyung.barleyssal_spring.domain.order;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OrderRejectReason {
    INSUFFICIENT_FUNDS("잔고 부족");

    private final String description;
}
