package com.taskqueue.www.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookTaskHandler implements TaskHandler {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Override
    public String getType() {
        return "WEBHOOK";
    }

    @Override
    public void handle(String message) throws Exception {

        JsonNode root = objectMapper.readTree(message);
        JsonNode json = root.path("data");

        String url = json.path("url").asText(null);

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("WEBHOOK → url is required");
        }

        String method = json.path("method").asText("POST").toUpperCase();

        JsonNode payloadNode = json.path("data");

        String payload = payloadNode.isMissingNode() || payloadNode.isNull()
                ? null
                : payloadNode.toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(method),
                entity,
                String.class
        );

        log.info("WEBHOOK SUCCESS → status={}", response.getStatusCode());
    }
}
