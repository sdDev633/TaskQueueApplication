package com.taskqueue.www.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskqueue.www.dto.*;
import com.taskqueue.www.enums.Role;
import com.taskqueue.www.model.Task;
import com.taskqueue.www.repository.TaskRepository;
import com.taskqueue.www.security.CustomUserDetails;
import com.taskqueue.www.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody TaskCreateRequest request) {

        // Build unified payload
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", request.getType());
        payload.set("data", request.getData());
        if (request.getMeta() != null) payload.set("meta", request.getMeta());

        // Save task
        Task task = new Task();
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setPayload(payload.toString());
        taskRepository.save(task);

        // Kafka message
        ObjectNode kafkaMsg = objectMapper.createObjectNode();
        kafkaMsg.put("taskId", task.getId());
        kafkaMsg.set("payload", payload);

        kafkaTemplate.send("task-topic", kafkaMsg.toString());

        return ResponseEntity.ok(Map.of(
                "taskId", task.getId(),
                "status", "QUEUED"
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TaskResponseDTO>>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(
                ApiResponse.success(taskService.getAllTasks(pageable))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(t -> ResponseEntity.ok(ApiResponse.success(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> getTaskStatus(@PathVariable Long id) {
        return taskService.getTaskStatus(id)
                .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TaskStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(taskService.getStats()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<TaskResponseDTO>>> getTasksByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(
                ApiResponse.success(taskService.getTasksByStatus(status, pageable)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> cancelTask(@PathVariable Long id) {
        return taskService.cancelTask(id)
                .map(t -> ResponseEntity.ok(ApiResponse.success("Task cancelled", t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> retryTask(@PathVariable Long id) {
        return taskService.retryTask(id)
                .map(t -> ResponseEntity.ok(ApiResponse.success("Task queued for retry", t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        return taskService.deleteTask(id)
                ? ResponseEntity.ok(ApiResponse.success("Task deleted", null))
                : ResponseEntity.notFound().build();
    }
}