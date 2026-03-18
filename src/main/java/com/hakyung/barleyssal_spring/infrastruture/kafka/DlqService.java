package com.hakyung.barleyssal_spring.infrastruture.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String DLQ_TOPIC = "service-common-dlq";

    public void sendToDlq(String originalTopic, String payload, String errorMessage, String errorType) {
        try{
        Map<String, Object> dlqMessage = new HashMap<>();
        dlqMessage.put("originalTopic", originalTopic);
        dlqMessage.put("payload", payload);
        dlqMessage.put("errorMessage", errorMessage);
        dlqMessage.put("errorType", errorType);
        dlqMessage.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(DLQ_TOPIC, objectMapper.writeValueAsString(dlqMessage));
        } catch (Exception e) {
            log.error("DLQ 전송 실패 : {}", e.getMessage());
        }
    }
}