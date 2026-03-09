package com.hakyung.barleyssal_spring.infrastrutrue.kafka;

import com.hakyung.barleyssal_spring.infrastrutrue.kafka.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    public static final String TOPIC_ORDER_REQUEST = "order.request";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(TOPIC_ORDER_REQUEST, event.orderId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish OrderPlacedEvent: orderId={}", event.orderId(), ex);
                } else {
                    log.debug("OrderPlacedEvent published: orderId={} partition={} offset={}",
                        event.orderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
