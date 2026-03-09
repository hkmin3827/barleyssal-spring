package com.hakyung.barleyssal_spring.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        var jsonSerializer = RedisSerializer.json();

        // 최신 자바 문법 기준으로 수정함. spring 4.x 버전에서 기존 문법 지원 X
        // 실제 EC2 배포 시에는 환경변수로 관리 권장
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, jsonSerializer);

        // 저사양 인프라 최적화: 배치 사이즈와 대기 시간 조절로 CPU/메모리 부하 감소
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10); // 10ms 대기 후 전송

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

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
