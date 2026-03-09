package com.hakyung.barleyssal_spring.domain.common.vo;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record StockCode(String value) implements Serializable {
    public StockCode {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("stockCode blank");
        value = value.trim().toUpperCase();
    }
    public static StockCode of(String v) { return new StockCode(v); }
    @Override public String toString()     { return value; }
}
