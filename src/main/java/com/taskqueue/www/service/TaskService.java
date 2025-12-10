package com.taskqueue.www.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskqueue.www.dto.OutboxStatusDTO;
import com.taskqueue.www.dto.TaskCreateRequestDTO;
import com.taskqueue.www.dto.TaskResponseDTO;
import com.taskqueue.www.dto.TaskStatsDTO;
import com.taskqueue.www.kafka.KafkaProducerService;
import com.taskqueue.www.model.OutboxEvent;
import com.taskqueue.www.model.Task;
import com.taskqueue.www.repository.OutboxRepository;
import com.taskqueue.www.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public TaskResponseDTO createTask(TaskCreateRequestDTO request) {
        // Step 1: Save task
        Task task = new Task();
        task.setPayload(request.getPayload());
        task.setStatus("PENDING");
        Task saved = taskRepository.save(task);

        // Step 2: Save to outbox table
        ObjectNode node = objectMapper.createObjectNode();
        node.put("taskId", saved.getId());
        node.put("payload", saved.getPayload());

        OutboxEvent event = new OutboxEvent();
        event.setPayload(node.toString());
        event.setStatus("NEW");
        event.setCreatedAt(LocalDateTime.now());
        OutboxEvent savedEvent = outboxRepository.save(event);

        return mapToDTO(saved, savedEvent);
    }

    public Page<TaskResponseDTO> getAllTasks(Pageable pageable) {
        Page<Task> tasks = taskRepository.findAll(pageable);
        return tasks.map(task -> {
            OutboxEvent outbox = findOutboxForTask(task.getId());
            return mapToDTO(task, outbox);
        });
    }

    public Optional<TaskResponseDTO> getTaskById(Long id) {
        return taskRepository.findById(id)
                .map(task -> {
                    OutboxEvent outbox = findOutboxForTask(task.getId());
                    return mapToDTO(task, outbox);
                });
    }

    public Optional<String> getTaskStatus(Long id) {
        return taskRepository.findById(id)
                .map(Task::getStatus);
    }

    public Page<TaskResponseDTO> getTasksByStatus(String status, Pageable pageable) {
        Page<Task> tasks = taskRepository.findByStatus(status, pageable);
        return tasks.map(task -> {
            OutboxEvent outbox = findOutboxForTask(task.getId());
            return mapToDTO(task, outbox);
        });
    }

    public TaskStatsDTO getStats() {
        long total = taskRepository.count();
        long pending = taskRepository.countByStatus("PENDING");
        long processing = taskRepository.countByStatus("PROCESSING");
        long done = taskRepository.countByStatus("DONE");
        long failed = taskRepository.countByStatus("FAILED");

        return new TaskStatsDTO(total, pending, processing, done, failed);
    }

    @Transactional
    public Optional<TaskResponseDTO> cancelTask(Long id) {
        return taskRepository.findById(id)
                .map(task -> {
                    if ("PENDING".equals(task.getStatus()) || "PROCESSING".equals(task.getStatus())) {
                        task.setStatus("CANCELLED");
                        Task saved = taskRepository.save(task);
                        OutboxEvent outbox = findOutboxForTask(saved.getId());
                        return mapToDTO(saved, outbox);
                    }
                    return mapToDTO(task, findOutboxForTask(task.getId()));
                });
    }

    @Transactional
    public Optional<TaskResponseDTO> retryTask(Long id) {
        return taskRepository.findById(id)
                .map(task -> {
                    if ("FAILED".equals(task.getStatus()) || "CANCELLED".equals(task.getStatus())) {
                        task.setStatus("PENDING");
                        Task saved = taskRepository.save(task);

                        // Create new outbox event for retry
                        ObjectNode node = objectMapper.createObjectNode();
                        node.put("taskId", saved.getId());
                        node.put("payload", saved.getPayload());

                        OutboxEvent event = new OutboxEvent();
                        event.setPayload(node.toString());
                        event.setStatus("NEW");
                        event.setCreatedAt(LocalDateTime.now());
                        OutboxEvent savedEvent = outboxRepository.save(event);

                        return mapToDTO(saved, savedEvent);
                    }
                    return mapToDTO(task, findOutboxForTask(task.getId()));
                });
    }

    @Transactional
    public boolean deleteTask(Long id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Helper methods
    private OutboxEvent findOutboxForTask(Long taskId) {
        // Find the most recent outbox event for this task
        String searchPattern = "\"taskId\":" + taskId;
        return outboxRepository.findAll().stream()
                .filter(e -> e.getPayload().contains(searchPattern))
                .findFirst()
                .orElse(null);
    }

    private TaskResponseDTO mapToDTO(Task task, OutboxEvent outbox) {
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setId(task.getId());
        dto.setPayload(task.getPayload());
        dto.setStatus(task.getStatus());

        if (outbox != null) {
            OutboxStatusDTO outboxStatus = new OutboxStatusDTO();
            outboxStatus.setStatus(outbox.getStatus());
            outboxStatus.setCreatedAt(outbox.getCreatedAt());
            dto.setOutboxStatus(outboxStatus);
        }

        return dto;
    }
}