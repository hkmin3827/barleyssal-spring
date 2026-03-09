package com.hakyung.barleyssal_spring.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.request")
                .partitions(1)
                .replicas(1)
                .config("retention.ms","3600000")
                .build();
    }

    @Bean
    public NewTopic executionEventTopic() {
        return TopicBuilder.name("execution.event")
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "3600000") // 통계 서버가 가져가면 바로 지워지도록 1시간으로 설정 (디스크 절약)
                .build();
    }
}
