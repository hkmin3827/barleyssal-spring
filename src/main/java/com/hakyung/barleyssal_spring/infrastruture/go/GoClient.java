package com.hakyung.barleyssal_spring.infrastruture.go;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GoClient {
    private final RestClient restClient = RestClient.create("http://localhost:4000");

    /** 체결 발생 시 Node.js 서버에 실시간 알림 브로드캐스트 요청 */
    public void notifyExecution(String orderId, String userId) {
        restClient.post()
                .uri("/api/broadcast/execution")
                .body(Map.of("orderId", orderId, "userId", userId))
                .retrieve()
                .toBodilessEntity();
    }
}