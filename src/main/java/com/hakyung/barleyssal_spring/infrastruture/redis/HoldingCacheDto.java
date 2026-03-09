package com.hakyung.barleyssal_spring.infrastruture.redis;

import java.math.BigDecimal;

public record HoldingCacheDto(
    BigDecimal avgPrice,
    long totalQty,
    long sellableQty
) {}