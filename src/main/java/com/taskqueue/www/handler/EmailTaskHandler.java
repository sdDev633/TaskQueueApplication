package com.taskqueue.www.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTaskHandler implements TaskHandler {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "EMAIL";
    }

    @Override
    public void handle(String data) throws Exception {

        JsonNode json = objectMapper.readTree(data);

        List<String> to = readList(json, "to");
        if (to.isEmpty()) {
            throw new IllegalArgumentException("EMAIL task must contain at least one recipient");
        }

        String subject = json.get("subject").asText();
        String body = json.get("body").asText();

        String from = json.has("from")
                ? json.get("from").asText()
                : "Local Task Engine <yourgmail@gmail.com>";

        boolean html = json.has("html") && json.get("html").asBoolean();

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

        helper.setFrom(from);
        helper.setTo(to.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(body, html);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("EMAIL SEND FAILED → {}", to, ex);
            throw ex; // IMPORTANT → task will be marked FAILED
        }
    }

    private List<String> readList(JsonNode json, String field) {
        if (!json.has(field) || !json.get(field).isArray()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        json.get(field).forEach(n -> list.add(n.asText()));
        return list;
    }
}
