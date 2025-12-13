package com.taskqueue.www.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueue.www.config.RetryConfig;
import com.taskqueue.www.handler.TaskHandler;
import com.taskqueue.www.handler.TaskHandlerRegistry;
import com.taskqueue.www.model.DeadLetterQueue;
import com.taskqueue.www.model.Task;
import com.taskqueue.www.repository.DeadLetterQueueRepository;
import com.taskqueue.www.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final TaskRepository taskRepository;
    private final TaskHandlerRegistry handlerRegistry;
    private final DeadLetterQueueRepository dlqRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "task-topic", groupId = "task-group")
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        Long taskId = null;
        try {
            String message = record.value();
            JsonNode node = objectMapper.readTree(message);
            taskId = node.get("taskId").asLong();
            String payload = node.has("payload") ? node.get("payload").asText() : null;

            Optional<Task> opt = taskRepository.findById(taskId);
            if (opt.isEmpty()) {
                log.error("No task record found for id={}", taskId);
                return;
            }

            Task task = opt.get();

            // Idempotency: if already DONE, skip
            if ("DONE".equalsIgnoreCase(task.getStatus())) {
                log.info("Task {} already DONE — skipping", taskId);
                return;
            }

            // Mark as processing
            task.setStatus("PROCESSING");
            task.setLastAttemptAt(LocalDateTime.now());
            taskRepository.save(task);

            // Parse the actual task payload
            JsonNode taskPayload = objectMapper.readTree(payload);
            String taskType = taskPayload.has("type")
                    ? taskPayload.get("type").asText()
                    : "DEFAULT";

            log.info("Processing taskId={} type={} attempt={}",
                    taskId, taskType, task.getRetryCount() + 1);

            try {
                // Route to appropriate handler
                TaskHandler handler = handlerRegistry.getHandler(taskType);
                if (handler != null) {
                    handler.handle(taskPayload.toString());
                } else {
                    log.warn("No handler found for task type: {}. Using default processing.", taskType);
                    Thread.sleep(2000); // Simulate work
                    log.info("Default processing completed for: {}", payload);
                }

                // Success! Mark as done
                task.setStatus("DONE");
                task.setErrorMessage(null);
                taskRepository.save(task);

                // If this task was retried from DLQ, mark DLQ as resolved
                updateDLQStatusIfRetried(taskId);

                log.info("Task {} completed successfully", taskId);

            } catch (Exception handlerException) {
                // Handler failed - initiate retry logic
                handleTaskFailure(task, handlerException, message);
            }

        } catch (Exception e) {
            log.error("Critical error processing Kafka message for taskId={}: {}",
                    taskId, e.getMessage(), e);

            // Try to mark task as failed if we have the taskId
            if (taskId != null) {
                taskRepository.findById(taskId).ifPresent(task -> {
                    task.setStatus("FAILED");
                    task.setErrorMessage("Critical error: " + e.getMessage());
                    taskRepository.save(task);
                    moveToDLQ(task, e);
                });
            }
        }
    }

    private void handleTaskFailure(Task task, Exception exception, String originalMessage) {
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(exception.getMessage());
        task.setLastAttemptAt(LocalDateTime.now());

        log.error("Task {} failed on attempt {}: {}",
                task.getId(), task.getRetryCount(), exception.getMessage());

        if (RetryConfig.shouldRetry(task.getRetryCount())) {
            // Schedule retry with exponential backoff
            task.setStatus("PENDING");
            taskRepository.save(task);

            long delayMs = RetryConfig.calculateBackoffDelay(task.getRetryCount());
            log.info("Scheduling retry {} for task {} in {}ms",
                    task.getRetryCount(), task.getId(), delayMs);

            scheduleRetry(originalMessage, delayMs);

        } else {
            // Max retries exceeded - move to DLQ
            task.setStatus("FAILED");
            taskRepository.save(task);

            log.error("Task {} failed permanently after {} attempts. Moving to DLQ.",
                    task.getId(), task.getRetryCount());

            moveToDLQ(task, exception);
        }
    }

    private void scheduleRetry(String message, long delayMs) {
        // Schedule retry by re-publishing to Kafka after delay
        // In production, use Kafka scheduled messages or a scheduler service
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                kafkaTemplate.send("task-topic", message);
                log.info("Retry message re-published to Kafka");
            } catch (InterruptedException e) {
                log.error("Retry scheduling interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void moveToDLQ(Task task, Exception exception) {
        try {
            DeadLetterQueue dlq = new DeadLetterQueue();
            dlq.setOriginalTaskId(task.getId());
            dlq.setPayload(task.getPayload());
            dlq.setTotalAttempts(task.getRetryCount());
            dlq.setLastError(exception.getMessage());
            dlq.setFailedAt(LocalDateTime.now());
            dlq.setStatus("FAILED");

            dlqRepository.save(dlq);

            log.info("Task {} moved to Dead Letter Queue (DLQ ID: {})",
                    task.getId(), dlq.getId());

        } catch (Exception e) {
            log.error("Failed to move task {} to DLQ: {}", task.getId(), e.getMessage());
        }
    }

    private void updateDLQStatusIfRetried(Long taskId) {
        try {
            taskRepository.findById(taskId).ifPresent(task -> {
                // Check if this task was retried from DLQ
                if (task.getRetriedFromDlqId() != null) {
                    dlqRepository.findById(task.getRetriedFromDlqId()).ifPresent(dlq -> {
                        dlq.setStatus("RESOLVED");
                        String resolution = dlq.getResolution() != null
                                ? dlq.getResolution() + " - Retry successful"
                                : "Retry successful";
                        dlq.setResolution(resolution);
                        dlqRepository.save(dlq);

                        log.info("DLQ item {} marked as RESOLVED after successful retry (task {})",
                                dlq.getId(), taskId);
                    });
                }
            });
        } catch (Exception e) {
            log.error("Failed to update DLQ status after successful retry: {}", e.getMessage());
        }
    }
}