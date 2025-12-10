package com.taskqueue.www.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
public class PdfTaskHandler implements TaskHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "PDF";
    }

    @Override
    public void handle(String data) throws Exception {
        JsonNode json = objectMapper.readTree(data);
        String template = json.get("template").asText();
        JsonNode documentData = json.get("data");

        log.info("Generating PDF with template: {}", template);

        // Create output directory if it doesn't exist
        Files.createDirectories(Paths.get("output/pdfs"));

        // For demo: Generate a simple text file (in production, use iText or similar)
        String filename = "output/pdfs/" + System.currentTimeMillis() + "_" + template + ".txt";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("=== PDF Document ===\n");
            writer.write("Template: " + template + "\n");
            writer.write("Data: " + documentData.toPrettyString() + "\n");
            writer.write("Generated at: " + java.time.LocalDateTime.now() + "\n");
        }

        log.info("PDF generated successfully: {}", filename);
        // TODO: Integrate real PDF library like iText or Flying Saucer
    }
}
