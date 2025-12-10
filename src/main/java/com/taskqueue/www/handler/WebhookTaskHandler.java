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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Override
    public String getType() {
        return "WEBHOOK";
    }

    @Override
    public void handle(String data) throws Exception {
        JsonNode json = objectMapper.readTree(data);
        String url = json.get("url").asText();
        String method = json.has("method") ? json.get("method").asText() : "POST";
        JsonNode payload = json.has("data") ? json.get("data") : null;

        log.info("Calling webhook: {} {}", method, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(
                payload != null ? payload.toString() : null,
                headers
        );

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(method),
                entity,
                String.class
        );

        log.info("Webhook response: {} - {}", response.getStatusCode(), response.getBody());
    }
}
