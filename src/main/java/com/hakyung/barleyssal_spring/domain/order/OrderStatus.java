package com.hakyung.barleyssal_spring.domain.order;

public enum OrderStatus {
    PENDING,    // 접수 대기
    SUBMITTED,  // Kafka 발행 완료
    FILLED,     // 체결 완료
    CANCELLED,
    REJECTED,
    EXPIRED
}
