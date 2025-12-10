package com.taskqueue.www.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailTaskHandler implements TaskHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "EMAIL";
    }

    @Override
    public void handle(String data) throws Exception {
        JsonNode json = objectMapper.readTree(data);
        String to = json.get("to").asText();
        String subject = json.get("subject").asText();
        String body = json.get("body").asText();

        log.info("Sending email to: {}, subject: {}", to, subject);

        // TODO: Integrate with real SMTP or use JavaMailSender
        // For demo, we'll simulate sending
        Thread.sleep(1000); // Simulate email sending time

        log.info("Email sent successfully to: {}", to);
    }
}