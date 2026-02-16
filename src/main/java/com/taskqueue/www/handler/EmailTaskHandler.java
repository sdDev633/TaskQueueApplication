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
    private final ObjectMapper objectMapper;

    @Override
    public String getType() {
        return "EMAIL";
    }

    @Override
    public void handle(String message) throws Exception {

        JsonNode root = objectMapper.readTree(message);
        JsonNode json = root.path("data");

        List<String> to = readList(json, "to");

        if (to.isEmpty()) {
            throw new IllegalArgumentException("EMAIL → at least one recipient required");
        }

        String subject = json.path("subject").asText("No Subject");
        String body = json.path("body").asText("");

        String from = json.path("from")
                .asText("Local Task Engine <yourgmail@gmail.com>");

        boolean html = json.path("html").asBoolean(false);

        MimeMessage messageObj = mailSender.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(messageObj, false, StandardCharsets.UTF_8.name());

        helper.setFrom(from);
        helper.setTo(to.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(body, html);

        mailSender.send(messageObj);

        log.info("EMAIL SENT SUCCESSFULLY → {}", to);
    }

    private List<String> readList(JsonNode json, String field) {

        JsonNode node = json.path(field);

        if (!node.isArray()) return List.of();

        List<String> list = new ArrayList<>();
        node.forEach(n -> list.add(n.asText()));
        return list;
    }
}
