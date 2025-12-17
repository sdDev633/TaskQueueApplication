package com.taskqueue.www.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.taskqueue.www.model.GeneratedDocument;
import com.taskqueue.www.repository.GeneratedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfTaskHandler implements TaskHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final PdfStorageService storageService;
    private final GeneratedDocumentRepository documentRepository;

    private static final String OUTPUT_DIR = "output/pdfs/";

    @Override
    public String getType() {
        return "PDF";
    }

    @Override
    public void handle(String data) throws Exception {
        JsonNode json = objectMapper.readTree(data);
        String template = json.get("template").asText();
        JsonNode documentData = json.get("data");
        Long taskId = json.has("taskId") ? json.get("taskId").asLong() : null;

        log.info("Generating PDF with template: {}", template);

        // Create output directory if it doesn't exist
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        // Generate filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = OUTPUT_DIR + timestamp + "_" + template + ".pdf";

        // Route to appropriate template handler
        switch (template.toLowerCase()) {
            case "invoice":
                generateInvoice(filename, documentData);
                break;
            case "receipt":
                generateReceipt(filename, documentData);
                break;
            case "report":
                generateReport(filename, documentData);
                break;
            default:
                generateGenericPdf(filename, template, documentData);
                break;
        }

        log.info("PDF generated successfully: {}", filename);
        log.info("File size: {} KB", new File(filename).length() / 1024);

        // NEW: Save document metadata to database
        saveDocumentMetadata(taskId, filename, template, new File(filename));
    }

    private void saveDocumentMetadata(Long taskId, String filename, String template, File pdfFile) {
        try {
            GeneratedDocument document = new GeneratedDocument();
            document.setTaskId(taskId);
            document.setFilename(pdfFile.getName());
            document.setDocumentType(template);
            document.setStorageType("LOCAL");
            document.setStoragePath(pdfFile.getAbsolutePath());
            document.setFileSizeBytes(pdfFile.length());
            document.setMimeType("application/pdf");
            document.setCreatedAt(LocalDateTime.now());

            documentRepository.save(document);

            log.info("Document metadata saved for task {}: {}", taskId, pdfFile.getName());

        } catch (Exception e) {
            log.error("Failed to save document metadata for task {}", taskId, e);
            // Don't throw - PDF is generated, just metadata tracking failed
        }
    }

    private void generateInvoice(String filename, JsonNode data) throws Exception {
        PdfWriter writer = new PdfWriter(filename);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Title
        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        Paragraph title = new Paragraph("INVOICE")
                .setFont(boldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // Invoice Details
        String invoiceNumber = data.has("invoiceNumber") ? data.get("invoiceNumber").asText() : "INV-001";
        String date = data.has("date") ? data.get("date").asText() : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String customerName = data.has("customerName") ? data.get("customerName").asText() : "Customer";

        document.add(new Paragraph("Invoice Number: " + invoiceNumber).setFontSize(12));
        document.add(new Paragraph("Date: " + date).setFontSize(12));
        document.add(new Paragraph("Customer: " + customerName).setFontSize(12).setMarginBottom(20));

        // Items Table
        Table table = new Table(new float[]{3, 1, 1, 1});
        table.setWidth(500);

        // Header
        table.addHeaderCell(new Cell().add(new Paragraph("Item").setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Qty").setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Price").setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Total").setFont(boldFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY));

        // Items
        double total = 0;
        if (data.has("items") && data.get("items").isArray()) {
            for (JsonNode item : data.get("items")) {
                String name = item.get("name").asText();
                int quantity = item.has("quantity") ? item.get("quantity").asInt() : 1;
                double price = item.has("price") ? item.get("price").asDouble() : 0;
                double itemTotal = quantity * price;
                total += itemTotal;

                table.addCell(name);
                table.addCell(String.valueOf(quantity));
                table.addCell(String.format("$%.2f", price));
                table.addCell(String.format("$%.2f", itemTotal));
            }
        }

        document.add(table);

        // Total
        Paragraph totalPara = new Paragraph(String.format("Total: $%.2f", total))
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(20);
        document.add(totalPara);

        // Footer
        document.add(new Paragraph("\nThank you for your business!")
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .setFontSize(10));

        document.close();
    }

    private void generateReceipt(String filename, JsonNode data) throws Exception {
        PdfWriter writer = new PdfWriter(filename);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

        // Title
        document.add(new Paragraph("RECEIPT")
                .setFont(boldFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Receipt Details
        String receiptNumber = data.has("receiptNumber") ? data.get("receiptNumber").asText() : "REC-001";
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        double amount = data.has("amount") ? data.get("amount").asDouble() : 0;
        String paymentMethod = data.has("paymentMethod") ? data.get("paymentMethod").asText() : "Cash";

        document.add(new Paragraph("Receipt #: " + receiptNumber));
        document.add(new Paragraph("Date: " + date));
        document.add(new Paragraph("Amount: $" + String.format("%.2f", amount)).setFont(boldFont).setFontSize(14));
        document.add(new Paragraph("Payment Method: " + paymentMethod));

        if (data.has("description")) {
            document.add(new Paragraph("\nDescription:"));
            document.add(new Paragraph(data.get("description").asText()));
        }

        document.add(new Paragraph("\n\nThank you!")
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20));

        document.close();
    }

    private void generateReport(String filename, JsonNode data) throws Exception {
        PdfWriter writer = new PdfWriter(filename);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

        // Title
        String reportTitle = data.has("title") ? data.get("title").asText() : "Report";
        document.add(new Paragraph(reportTitle)
                .setFont(boldFont)
                .setFontSize(22)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Date
        document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(20));

        // Summary
        if (data.has("summary")) {
            document.add(new Paragraph("Summary").setFont(boldFont).setFontSize(14));
            document.add(new Paragraph(data.get("summary").asText()).setMarginBottom(15));
        }

        // Metrics
        if (data.has("metrics") && data.get("metrics").isObject()) {
            document.add(new Paragraph("Key Metrics").setFont(boldFont).setFontSize(14).setMarginTop(10));

            JsonNode metrics = data.get("metrics");
            metrics.fields().forEachRemaining(entry -> {
                try {
                    document.add(new Paragraph(entry.getKey() + ": " + entry.getValue().asText()));
                } catch (Exception e) {
                    log.error("Error adding metric", e);
                }
            });
        }

        // Content
        if (data.has("content")) {
            document.add(new Paragraph("\nDetails").setFont(boldFont).setFontSize(14).setMarginTop(20));
            document.add(new Paragraph(data.get("content").asText()));
        }

        document.close();
    }

    private void generateGenericPdf(String filename, String template, JsonNode data) throws Exception {
        PdfWriter writer = new PdfWriter(filename);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

        // Title
        document.add(new Paragraph(template.toUpperCase())
                .setFont(boldFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Date
        document.add(new Paragraph("Generated: " + LocalDateTime.now())
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(20));

        // Content
        document.add(new Paragraph("Data:").setFont(boldFont).setFontSize(12));
        document.add(new Paragraph(data.toPrettyString())
                .setFontSize(10)
                .setMarginTop(10));

        document.close();
    }
}
