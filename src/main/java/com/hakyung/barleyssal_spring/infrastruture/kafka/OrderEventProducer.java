package com.hakyung.barleyssal_spring.infrastruture.kafka;

import com.hakyung.barleyssal_spring.infrastruture.kafka.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    public static final String TOPIC_ORDER_REQUEST = "order.request";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(OrderCreatedEvent event) {
        String payload = objectMapper.writeValueAsString(event);
        try {
            kafkaTemplate.send(TOPIC_ORDER_REQUEST, event.orderId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderPlacedEvent: orderId={}", event.orderId(), ex);
                    } else  {
                        log.info("Published OrderPlacedEvent: orderId={}", event.orderId());
                    }
            });
        } catch (Exception e) {
            log.error("Failed to serialize OrderCreatedEvent : {}", e.getMessage());
        }
    }
}
