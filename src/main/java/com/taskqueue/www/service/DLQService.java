package com.taskqueue.www.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskqueue.www.dto.*;
import com.taskqueue.www.model.DeadLetterQueue;
import com.taskqueue.www.model.OutboxEvent;
import com.taskqueue.www.model.Task;
import com.taskqueue.www.repository.DeadLetterQueueRepository;
import com.taskqueue.www.repository.OutboxRepository;
import com.taskqueue.www.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DLQService {

    private final DeadLetterQueueRepository dlqRepository;
    private final TaskRepository taskRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Page<DLQResponseDTO> getAllDLQ(Pageable pageable) {
        return dlqRepository.findAll(pageable).map(this::mapToDTO);
    }

    public Optional<DLQResponseDTO> getDLQById(Long id) {
        return dlqRepository.findById(id).map(this::mapToDTO);
    }

    public Page<DLQResponseDTO> getDLQByStatus(String status, Pageable pageable) {
        return dlqRepository.findByStatus(status, pageable).map(this::mapToDTO);
    }

    public DLQStatsDTO getDLQStats() {
        long totalFailed = dlqRepository.countByStatus("FAILED");
        long retrying = dlqRepository.countByStatus("RETRYING");
        long resolved = dlqRepository.countByStatus("RESOLVED");

        return new DLQStatsDTO(totalFailed, retrying, resolved);
    }

    @Transactional
    public Optional<TaskResponseDTO> retryDLQTask(Long dlqId, RetryRequestDTO request) {
        return dlqRepository.findById(dlqId).map(dlq -> {
            // Update DLQ status
            dlq.setStatus("RETRYING");
            if (request != null && request.getResolution() != null) {
                dlq.setResolution(request.getResolution());
            }
            dlqRepository.save(dlq);

            // Get or create task
            Task task;
            if (dlq.getOriginalTaskId() != null) {
                task = taskRepository.findById(dlq.getOriginalTaskId())
                        .orElse(createNewTask(dlq.getPayload()));
            } else {
                task = createNewTask(dlq.getPayload());
            }

            // Reset task for retry
            task.setStatus("PENDING");
            task.setRetryCount(0);
            task.setErrorMessage(null);
            Task savedTask = taskRepository.save(task);

            // Create outbox event for retry
            ObjectNode node = objectMapper.createObjectNode();
            node.put("taskId", savedTask.getId());
            node.put("payload", savedTask.getPayload());

            OutboxEvent event = new OutboxEvent();
            event.setPayload(node.toString());
            event.setStatus("NEW");
            event.setCreatedAt(LocalDateTime.now());
            OutboxEvent savedEvent = outboxRepository.save(event);

            log.info("DLQ item {} retried as task {}", dlqId, savedTask.getId());

            return mapTaskToDTO(savedTask, savedEvent);
        });
    }

    @Transactional
    public Optional<DLQResponseDTO> resolveDLQ(Long dlqId, String resolution) {
        return dlqRepository.findById(dlqId).map(dlq -> {
            dlq.setStatus("RESOLVED");
            dlq.setResolution(resolution != null ? resolution : "Manually resolved");
            DeadLetterQueue saved = dlqRepository.save(dlq);

            log.info("DLQ item {} marked as resolved", dlqId);

            return mapToDTO(saved);
        });
    }

    @Transactional
    public boolean deleteDLQItem(Long id) {
        if (dlqRepository.existsById(id)) {
            dlqRepository.deleteById(id);
            log.info("DLQ item {} deleted", id);
            return true;
        }
        return false;
    }

    // Helper methods
    private Task createNewTask(String payload) {
        Task task = new Task();
        task.setPayload(payload);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    private DLQResponseDTO mapToDTO(DeadLetterQueue dlq) {
        DLQResponseDTO dto = new DLQResponseDTO();
        dto.setId(dlq.getId());
        dto.setOriginalTaskId(dlq.getOriginalTaskId());
        dto.setPayload(dlq.getPayload());
        dto.setTotalAttempts(dlq.getTotalAttempts());
        dto.setLastError(dlq.getLastError());
        dto.setFailedAt(dlq.getFailedAt());
        dto.setStatus(dlq.getStatus());
        dto.setResolution(dlq.getResolution());
        return dto;
    }

    private TaskResponseDTO mapTaskToDTO(Task task, OutboxEvent outbox) {
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