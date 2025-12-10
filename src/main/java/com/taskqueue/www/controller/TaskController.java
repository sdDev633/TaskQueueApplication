package com.taskqueue.www.controller;

import com.taskqueue.www.dto.ApiResponse;
import com.taskqueue.www.dto.TaskCreateRequestDTO;
import com.taskqueue.www.dto.TaskResponseDTO;
import com.taskqueue.www.dto.TaskStatsDTO;
import com.taskqueue.www.model.Task;
import com.taskqueue.www.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponseDTO>> createTask(
            @RequestBody TaskCreateRequestDTO request) {
        TaskResponseDTO task = taskService.createTask(request);
        return ResponseEntity.ok(ApiResponse.success("Task created and queued", task));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TaskResponseDTO>>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<TaskResponseDTO> tasks = taskService.getAllTasks(pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(task -> ResponseEntity.ok(ApiResponse.success(task)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> getTaskStatus(@PathVariable Long id) {
        return taskService.getTaskStatus(id)
                .map(status -> ResponseEntity.ok(ApiResponse.success(status)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TaskStatsDTO>> getStats() {
        TaskStatsDTO stats = taskService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<TaskResponseDTO>>> getTasksByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<TaskResponseDTO> tasks = taskService.getTasksByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> cancelTask(@PathVariable Long id) {
        return taskService.cancelTask(id)
                .map(task -> ResponseEntity.ok(ApiResponse.success("Task cancelled", task)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> retryTask(@PathVariable Long id) {
        return taskService.retryTask(id)
                .map(task -> ResponseEntity.ok(ApiResponse.success("Task queued for retry", task)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        boolean deleted = taskService.deleteTask(id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("Task deleted", null));
        }
        return ResponseEntity.notFound().build();
    }
}