package com.hakyung.barleyssal_spring.domain.shared;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
    String aggregateType();
    String aggregateId();
}
