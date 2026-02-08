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

        Long taskId = null;   // ✅ visible everywhere

        try {
            JsonNode msg = objectMapper.readTree(record.value());

            taskId = msg.get("taskId").asLong();
            final Long finalTaskId = taskId;   // ✅ lambda-safe copy

            JsonNode payloadNode = msg.get("payload");

            Task task = taskRepository.findById(finalTaskId)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + finalTaskId));

            if ("DONE".equalsIgnoreCase(task.getStatus())) {
                log.info("Task {} already DONE", finalTaskId);
                return;
            }

            task.setStatus("PROCESSING");
            task.setLastAttemptAt(LocalDateTime.now());
            taskRepository.save(task);

            String type = payloadNode.get("type").asText();
            JsonNode dataNode = payloadNode.get("data");

            TaskHandler handler = handlerRegistry.getHandler(type);
            if (handler == null) {
                throw new RuntimeException("No handler registered for type: " + type);
            }

            handler.handle(dataNode.toString());

            task.setStatus("DONE");
            task.setErrorMessage(null);
            taskRepository.save(task);

            updateDLQStatusIfRetried(finalTaskId);

            log.info("Task {} executed successfully [{}]", finalTaskId, type);

        } catch (Exception e) {

            log.error("Task execution failed for taskId={}", taskId, e);

            if (taskId != null) {
                final Long finalTaskId = taskId;   // ✅ lambda-safe again

                taskRepository.findById(finalTaskId).ifPresent(task ->
                        handleTaskFailure(task, e, record.value())
                );
            }
        }
    }


    private void handleTaskFailure(Task task, Exception exception, String originalMessage) {

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
                    log.error("Retry failed", e);
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
