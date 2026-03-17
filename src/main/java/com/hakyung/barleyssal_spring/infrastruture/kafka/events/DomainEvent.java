package com.hakyung.barleyssal_spring.infrastruture.kafka.events;

public interface DomainEvent {
    Long occurredAt();
    String aggregateType();
    String aggregateId();
}
