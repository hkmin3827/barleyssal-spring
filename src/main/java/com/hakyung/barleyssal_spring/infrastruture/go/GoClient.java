package com.hakyung.barleyssal_spring.infrastruture.go;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GoClient {
    private final RestClient restClient = RestClient.create("http://localhost:4000");

    public void notifyExecution(String orderId, String userId) {
        restClient.post()
                .uri("/api/broadcast/execution")
                .body(Map.of("orderId", orderId, "userId", userId))
                .retrieve()
                .toBodilessEntity();
    }
}