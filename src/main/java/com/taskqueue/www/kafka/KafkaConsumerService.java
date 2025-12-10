package com.taskqueue.www.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueue.www.handler.TaskHandler;
import com.taskqueue.www.handler.TaskHandlerRegistry;
import com.taskqueue.www.model.Task;
import com.taskqueue.www.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final TaskRepository taskRepository;
    private final TaskHandlerRegistry handlerRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "task-topic", groupId = "task-group")
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        try {
            String message = record.value();
            JsonNode node = objectMapper.readTree(message);
            long taskId = node.get("taskId").asLong();
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
            taskRepository.save(task);

            // Parse the actual task payload
            JsonNode taskPayload = objectMapper.readTree(payload);
            String taskType = taskPayload.has("type")
                    ? taskPayload.get("type").asText()
                    : "DEFAULT";

            log.info("Processing taskId={} type={}", taskId, taskType);

            // Route to appropriate handler
            TaskHandler handler = handlerRegistry.getHandler(taskType);
            if (handler != null) {
                handler.handle(taskPayload.toString());
            } else {
                log.warn("No handler found for task type: {}. Using default processing.", taskType);
                // Default behavior: just log the payload
                Thread.sleep(2000); // Simulate work
                log.info("Default processing completed for: {}", payload);
            }

            // Mark as done
            task.setStatus("DONE");
            taskRepository.save(task);

            log.info("Task {} completed successfully", taskId);

        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", e.getMessage(), e);
            // TODO: Implement retry logic and DLQ here
        }
    }
}