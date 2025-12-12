package com.taskqueue.www.controller;

import com.taskqueue.www.dto.*;
import com.taskqueue.www.service.DLQService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
public class DLQController {

    private final DLQService dlqService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DLQResponseDTO>>> getAllDLQ(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "failedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DLQResponseDTO> dlqItems = dlqService.getAllDLQ(pageable);
        return ResponseEntity.ok(ApiResponse.success(dlqItems));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DLQResponseDTO>> getDLQById(@PathVariable Long id) {
        return dlqService.getDLQById(id)
                .map(dlq -> ResponseEntity.ok(ApiResponse.success(dlq)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<DLQResponseDTO>>> getDLQByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "failedAt"));
        Page<DLQResponseDTO> dlqItems = dlqService.getDLQByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(dlqItems));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DLQStatsDTO>> getDLQStats() {
        DLQStatsDTO stats = dlqService.getDLQStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PutMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> retryDLQTask(
            @PathVariable Long id,
            @RequestBody(required = false) RetryRequestDTO request) {

        return dlqService.retryDLQTask(id, request)
                .map(task -> ResponseEntity.ok(
                        ApiResponse.success("Task re-queued for retry", task)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<DLQResponseDTO>> resolveDLQ(
            @PathVariable Long id,
            @RequestBody RetryRequestDTO request) {

        return dlqService.resolveDLQ(id, request.getResolution())
                .map(dlq -> ResponseEntity.ok(
                        ApiResponse.success("DLQ item marked as resolved", dlq)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDLQItem(@PathVariable Long id) {
        boolean deleted = dlqService.deleteDLQItem(id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("DLQ item deleted", null));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/update-payload")
    public ResponseEntity<ApiResponse<DLQResponseDTO>> updatePayload(
            @PathVariable Long id,
            @RequestBody UpdatePayloadRequestDTO request) {

        return dlqService.updatePayload(id, request)
                .map(dlq -> ResponseEntity.ok(
                        ApiResponse.success("Payload updated successfully", dlq)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/retry-all")
    public ResponseEntity<ApiResponse<BulkRetryResponseDTO>> retryAllFailed(
            @RequestBody(required = false) RetryRequestDTO request) {

        BulkRetryResponseDTO result = dlqService.retryAllFailed(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/retry-all/status/{status}")
    public ResponseEntity<ApiResponse<BulkRetryResponseDTO>> retryByStatus(
            @PathVariable String status,
            @RequestBody(required = false) RetryRequestDTO request) {

        BulkRetryResponseDTO result = dlqService.retryByStatus(status, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}