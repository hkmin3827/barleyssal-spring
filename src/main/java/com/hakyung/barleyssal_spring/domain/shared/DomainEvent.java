package com.hakyung.barleyssal_spring.domain.shared;

import java.time.Instant;

public interface DomainEvent {
    Long eventId();
    Instant occurredAt();
    String aggregateType();
    String aggregateId();
}
