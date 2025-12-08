package com.taskqueue.www.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueue.www.model.Task;
import com.taskqueue.www.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final TaskRepository taskRepository;
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
                // no task found: log and skip
                System.err.println("No task record found for id=" + taskId);
                return;
            }

            Task task = opt.get();

            // idempotency: if already DONE, skip
            if ("DONE".equalsIgnoreCase(task.getStatus())) {
                System.out.println("Task " + taskId + " already DONE — skipping");
                return;
            }

            // mark processing
            task.setStatus("PROCESSING");
            taskRepository.save(task);

            // ----- Do real work here -----
            System.out.println("Processing taskId=" + taskId + " payload=" + payload);
            // simulate work or call service
            // -------------------------------

            // mark done
            task.setStatus("DONE");
            taskRepository.save(task);

        } catch (Exception e) {
            // handle exception: you can set FAILED and/or rely on retries
            System.err.println("Error processing Kafka message: " + e.getMessage());
            // optionally attempt to set task FAILED if taskId present
        }
    }
}