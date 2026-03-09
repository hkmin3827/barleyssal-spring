package com.hakyung.barleyssal_spring.infrastrutrue.node;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class NodeIconClient implements IconProcessor {
    private final RestClient restClient;

    @Value("${external.node-server.url}")
    private String nodeServerUrl;

    @Override
    public void process(String s3Key, Long userId) {
        NodeConvertRequest req = new NodeConvertRequest(s3Key, userId);

        restClient.post()
                .uri(nodeServerUrl + "/api/v1/convert")
                .body(req)
                .retrieve()
                .toBodilessEntity();
    }
}
