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
        JsonNode json = root.path("data");  // This gets {url, method, body}

        String url = json.path("url").asText(null);
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("WEBHOOK → url is required");
        }

        String method = json.path("method").asText("POST").toUpperCase();

        // ✅ FIX: Get the message from json.path("body").path("message")
        JsonNode bodyNode = json.path("body");
        String payload = null;

        if (!bodyNode.isMissingNode() && !bodyNode.isNull()) {
            // If you want to send just the message value:
            // payload = bodyNode.path("message").asText();

            // Or if you want to send the entire body object:
            payload = bodyNode.toString();
        }

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
