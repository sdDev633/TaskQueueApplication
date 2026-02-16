package com.taskqueue.www.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.taskqueue.www.dto.InvoiceData;
import com.taskqueue.www.dto.InvoiceItem;
import com.taskqueue.www.model.GeneratedDocument;
import com.taskqueue.www.repository.GeneratedDocumentRepository;
import com.taskqueue.www.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfTaskHandler implements TaskHandler {

    private final ObjectMapper objectMapper;
    private final PdfGeneratorService pdfGeneratorService;
    private final GeneratedDocumentRepository documentRepository;

    @Override
    public String getType() {
        return "PDF";
    }

    @Override
    public void handle(String message) throws Exception {

        JsonNode root = objectMapper.readTree(message);

        JsonNode dataNode = root.path("data");

        if (dataNode.isMissingNode() || dataNode.isNull()) {
            throw new RuntimeException("data section missing");
        }

        String template = dataNode.path("template").asText(null);
        String fileName = dataNode.path("fileName").asText(null);

        JsonNode invoiceDataNode = dataNode.path("data");

        if (invoiceDataNode.isMissingNode() || invoiceDataNode.isNull()) {
            throw new RuntimeException("invoice data missing");
        }

        InvoiceData invoice =
                objectMapper.treeToValue(invoiceDataNode, InvoiceData.class);

        pdfGeneratorService.generateInvoicePdf(invoice, fileName);

        log.info("PDF GENERATED SUCCESSFULLY → {}", fileName);
    }

    private void saveMetadata(Long taskId, String pdfPath, String template) {

        try {
            File pdfFile = new File(pdfPath);

            GeneratedDocument doc = new GeneratedDocument();
            doc.setTaskId(taskId);
            doc.setFilename(pdfFile.getName());
            doc.setDocumentType(template);
            doc.setStorageType("LOCAL");
            doc.setStoragePath(pdfFile.getAbsolutePath());
            doc.setFileSizeBytes(pdfFile.length());
            doc.setMimeType("application/pdf");
            doc.setCreatedAt(LocalDateTime.now());

            documentRepository.save(doc);

        } catch (Exception e) {
            log.error("DOCUMENT METADATA SAVE FAILED", e);
        }
    }
}
