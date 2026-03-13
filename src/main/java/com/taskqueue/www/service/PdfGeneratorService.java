package com.taskqueue.www.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
//import com.taskqueue.www.dto.InvoiceData;
import com.taskqueue.www.dto.InvoiceItem;
import com.taskqueue.www.model.InvoiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class PdfGeneratorService {

    private static final String UPLOAD_DIR = "generated-pdfs";

    public String generateInvoicePdf(InvoiceData invoice, String fileName) {

        try {

            Path uploadPath = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            if (fileName == null || fileName.isBlank()) {
                fileName = "invoice-" + UUID.randomUUID() + ".pdf";
            }

            Path filePath = uploadPath.resolve(fileName);

            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath.toFile()));
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            document.add(new Paragraph("INVOICE"));
            document.add(new Paragraph("Invoice No: " + invoice.getInvoiceNumber()));
            document.add(new Paragraph("Date: " + invoice.getInvoiceDate()));
            document.add(new Paragraph("Customer: " + invoice.getCustomerName()));
            document.add(new Paragraph(" "));

            Table table = new Table(4);

            table.addCell(new Paragraph("Item"));
            table.addCell(new Paragraph("Qty"));
            table.addCell(new Paragraph("Price"));
            table.addCell(new Paragraph("Total"));

            for (InvoiceData.Item item : invoice.getItems()) {
                table.addCell(new Paragraph(item.getName()));
                table.addCell(new Paragraph(String.valueOf(item.getQty())));
                table.addCell(new Paragraph(String.valueOf(item.getPrice())));
                table.addCell(new Paragraph(String.valueOf(item.getTotal())));
            }

            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Grand Total: " + invoice.getTotal()));

            document.close();

            log.info("PDF GENERATED: {}", filePath);

            return filePath.toString();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }
}
