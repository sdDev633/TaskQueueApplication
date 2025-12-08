package com.taskqueue.www.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskqueue.www.kafka.KafkaProducerService;
import com.taskqueue.www.model.OutboxEvent;
import com.taskqueue.www.model.Task;
import com.taskqueue.www.repository.OutboxRepository;
import com.taskqueue.www.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public String enqueue(Task task) {
        // Step 1: Save task
        task.setStatus("PENDING");
        Task saved = taskRepository.save(task);

        // Step 2: Save to outbox table with taskId
        ObjectNode node = objectMapper.createObjectNode();
        node.put("taskId", saved.getId());
        node.put("payload", saved.getPayload());

        OutboxEvent event = new OutboxEvent();
        event.setPayload(node.toString());
        event.setStatus("NEW");
        outboxRepository.save(event);

        return "Task saved & queued (pending to send to Kafka)";
    }
}