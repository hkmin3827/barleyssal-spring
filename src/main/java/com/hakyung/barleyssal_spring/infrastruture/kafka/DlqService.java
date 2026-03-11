package com.hakyung.barleyssal_spring.infrastruture.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DlqService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String DLQ_TOPIC = "service-common-dlq";

    public void sendToDlq(String originalTopic, String payload, String errorMessage, String errorType) {
        Map<String, Object> dlqMessage = new HashMap<>();
        dlqMessage.put("originalTopic", originalTopic);
        dlqMessage.put("payload", payload);
        dlqMessage.put("errorMessage", errorMessage);
        dlqMessage.put("errorType", errorType);
        dlqMessage.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(DLQ_TOPIC, dlqMessage);
    }
}