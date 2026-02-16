package com.taskqueue.www.kafka;

import com.taskqueue.www.model.OutboxEvent;
import com.taskqueue.www.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRepository outboxRepository;

    public void sendTask(OutboxEvent event) {

        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(
                        "task-topic",
                        String.valueOf(event.getTaskId()),
                        event.getPayload()
                );

        future.whenComplete((result, ex) -> {

            if (ex == null) {
                event.setStatus("SENT");
            } else {
                event.setStatus("NEW"); // retry
            }

            outboxRepository.save(event);
        });
    }

}
