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
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "task-topic", groupId = "task-group")
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {

        log.info("KAFKA MESSAGE RECEIVED: {}", record.value());

        try {

            JsonNode msg = objectMapper.readTree(record.value());

            Long taskId = msg.get("taskId").asLong();
            JsonNode payloadNode = msg.get("payload");

            String type = payloadNode.get("type").asText();

            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

            // ✅ idempotency guards
            if ("DONE".equalsIgnoreCase(task.getStatus())
                    || "CANCELLED".equalsIgnoreCase(task.getStatus())) {
                return;
            }

            task.setStatus("PROCESSING");
            task.setLastAttemptAt(LocalDateTime.now());
            taskRepository.save(task);

            TaskHandler handler = handlerRegistry.getHandler(type);

            if (handler == null) {
                throw new RuntimeException("No handler for type: " + type);
            }

            // ✅ pass ONLY payload
            handler.handle(payloadNode.toString());

            task.setStatus("DONE");
            task.setErrorMessage(null);
            taskRepository.save(task);

            updateDLQStatusIfRetried(taskId);

            log.info("TASK COMPLETED SUCCESSFULLY: {}", taskId);

        } catch (Exception e) {

            log.error("TASK EXECUTION FAILED. RAW MESSAGE={}", record.value(), e);

            try {
                JsonNode msg = objectMapper.readTree(record.value());
                Long taskId = msg.get("taskId").asLong();

                taskRepository.findById(taskId)
                        .ifPresent(task -> handleTaskFailure(task, e, record.value()));

            } catch (Exception ex) {
                log.error("CRITICAL FAILURE HANDLER ERROR", ex);
            }
        }
    }

    private void handleTaskFailure(Task task, Exception exception, String originalMessage) {

        log.warn("HANDLING FAILURE: taskId={}, attempt={}",
                task.getId(), task.getRetryCount() + 1);

        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(exception.getMessage());
        task.setLastAttemptAt(LocalDateTime.now());

        if (task.getRetryCount() <= task.getMaxRetries()) {

            task.setStatus("PENDING");
            taskRepository.save(task);

            long delay = RetryConfig.calculateBackoffDelay(task.getRetryCount());

            new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    kafkaTemplate.send("task-topic", originalMessage);
                } catch (Exception e) {
                    log.error("RETRY FAILED: taskId={}", task.getId(), e);
                }
            }).start();

        } else {

            task.setStatus("FAILED");
            taskRepository.save(task);
            moveToDLQ(task, exception);
        }
    }

    private void moveToDLQ(Task task, Exception exception) {

        DeadLetterQueue dlq = new DeadLetterQueue();
        dlq.setOriginalTaskId(task.getId());
        dlq.setPayload(task.getPayload());
        dlq.setTotalAttempts(task.getRetryCount());
        dlq.setLastError(exception.getMessage());
        dlq.setFailedAt(LocalDateTime.now());
        dlq.setStatus("FAILED");

        dlqRepository.save(dlq);
    }

    private void updateDLQStatusIfRetried(Long taskId) {

        taskRepository.findById(taskId).ifPresent(task -> {
            if (task.getRetriedFromDlqId() != null) {

                dlqRepository.findById(task.getRetriedFromDlqId()).ifPresent(dlq -> {
                    dlq.setStatus("RESOLVED");
                    dlq.setResolution("Retry successful");
                    dlqRepository.save(dlq);
                });
            }
        });
    }
}
