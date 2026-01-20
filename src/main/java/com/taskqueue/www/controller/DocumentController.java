package com.taskqueue.www.controller;


import com.taskqueue.www.dto.ApiResponse;
import com.taskqueue.www.model.GeneratedDocument;
import com.taskqueue.www.repository.GeneratedDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final GeneratedDocumentRepository documentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GeneratedDocument>>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<GeneratedDocument> documents = documentRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GeneratedDocument>> getDocumentById(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(doc -> ResponseEntity.ok(ApiResponse.success(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<ApiResponse<GeneratedDocument>> getDocumentByTaskId(@PathVariable Long taskId) {
        return documentRepository.findByTaskId(taskId)
                .map(doc -> ResponseEntity.ok(ApiResponse.success(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{documentType}")
    public ResponseEntity<ApiResponse<Page<GeneratedDocument>>> getDocumentsByType(
            @PathVariable String documentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GeneratedDocument> documents = documentRepository.findByDocumentType(documentType, pageable);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDocumentStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", documentRepository.count());
        stats.put("invoices", documentRepository.countByDocumentType("invoice"));
        stats.put("receipts", documentRepository.countByDocumentType("receipt"));
        stats.put("reports", documentRepository.countByDocumentType("report"));

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(doc -> {
                    File file = new File(doc.getStoragePath());
                    if (!file.exists()) {
                        return ResponseEntity.notFound().<Resource>build();
                    }

                    Resource resource = new FileSystemResource(file);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + doc.getFilename() + "\"")
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(doc -> {
                    File file = new File(doc.getStoragePath());
                    if (!file.exists()) {
                        return ResponseEntity.notFound().<Resource>build();
                    }

                    Resource resource = new FileSystemResource(file);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + doc.getFilename() + "\"")
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(doc -> {
                    // Delete file from disk
                    File file = new File(doc.getStoragePath());
                    if (file.exists()) {
                        file.delete();
                    }

                    // Delete from database
                    documentRepository.deleteById(id);

                    return ResponseEntity.ok("Document deleted successfully");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}