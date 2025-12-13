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
import java.util.List;
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

            // ALWAYS use the updated DLQ payload (not old task)
            // This ensures any payload updates are used in the retry
            Task task = createNewTask(dlq.getPayload());
            task.setRetriedFromDlqId(dlqId); // Track which DLQ this came from

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

    @Transactional
    public Optional<DLQResponseDTO> updatePayload(Long dlqId, UpdatePayloadRequestDTO request) {
        return dlqRepository.findById(dlqId).map(dlq -> {
            // Update the payload
            dlq.setPayload(request.getPayload());

            // Add resolution note
            String resolutionNote = request.getResolution() != null
                    ? request.getResolution()
                    : "Payload updated";
            dlq.setResolution(resolutionNote);

            DeadLetterQueue saved = dlqRepository.save(dlq);

            log.info("DLQ item {} payload updated", dlqId);

            return mapToDTO(saved);
        });
    }

    @Transactional
    public BulkRetryResponseDTO retryAllFailed(RetryRequestDTO request) {
        return retryByStatus("FAILED", request);
    }

    @Transactional
    public BulkRetryResponseDTO retryByStatus(String status, RetryRequestDTO request) {
        List<DeadLetterQueue> dlqItems = dlqRepository.findAll().stream()
                .filter(dlq -> status.equalsIgnoreCase(dlq.getStatus()))
                .toList();

        int totalRetried = 0;
        int successful = 0;
        int failed = 0;

        for (DeadLetterQueue dlq : dlqItems) {
            try {
                totalRetried++;

                // Update DLQ status
                dlq.setStatus("RETRYING");
                if (request != null && request.getResolution() != null) {
                    dlq.setResolution(request.getResolution());
                }
                dlqRepository.save(dlq);

                // ALWAYS use the updated DLQ payload (not old task)
                Task task = createNewTask(dlq.getPayload());
                task.setRetriedFromDlqId(dlq.getId()); // Track which DLQ this came from

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
                outboxRepository.save(event);

                successful++;
                log.info("DLQ item {} retried successfully as task {}", dlq.getId(), savedTask.getId());

            } catch (Exception e) {
                failed++;
                log.error("Failed to retry DLQ item {}: {}", dlq.getId(), e.getMessage());
            }
        }

        String message = String.format("Bulk retry completed: %d total, %d successful, %d failed",
                totalRetried, successful, failed);

        log.info(message);

        return new BulkRetryResponseDTO(totalRetried, successful, failed, message);
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