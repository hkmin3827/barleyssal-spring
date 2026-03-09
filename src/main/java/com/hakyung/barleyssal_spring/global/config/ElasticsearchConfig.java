package com.hakyung.barleyssal_spring.global.config;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@EnableElasticsearchRepositories(basePackages = "com.hakyung.barleyssal_spring.infrastruture.elastic")
public class ElasticsearchConfig extends ElasticsearchConfiguration {
    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Override
    public ClientConfiguration clientConfiguration() {
        // "http://" 문자열 제거 (ClientConfiguration.builder는 호스트:포트 형식 기대)
        String cleanUri = elasticsearchUri.replace("http://", "").replace("https://", "");

        return ClientConfiguration.builder()
                .connectedTo(cleanUri)
//                .usingSsl(false)
//                .withDefaultHeaders(new HttpHeaders() {{
//                    add("Accept", "application/vnd.elasticsearch+json; compatible-with=8");
//                    add("Content-Type", "application/vnd.elasticsearch+json; compatible-with=8");
//                }})
                // 저사양 인프라 최적화: 타임아웃을 타이트하게 설정하여 불필요한 대기 스레드 방지
                .withConnectTimeout(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public JacksonJsonpMapper jsonpMapper() {
        // 주식 데이터(BigDecimal) 및 시간 처리를 위한 최적화된 매퍼 구성
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule()) // JDK 25 및 LocalDateTime 지원
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 가독성 높은 ISO 형식 사용

        return new JacksonJsonpMapper(mapper);
    }
}