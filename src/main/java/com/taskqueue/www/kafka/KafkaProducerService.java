package com.taskqueue.www.kafka;

import com.taskqueue.www.model.OutboxEvent;
import com.taskqueue.www.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRepository outboxRepository;

    public void sendTask(OutboxEvent event) {

        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send("task-topic", event.getPayload());

        future.whenComplete((result, ex) -> {

            if (ex == null) {
                // SUCCESS
                event.setStatus("SENT");
                outboxRepository.save(event);

                System.out.println("Kafka ACK for outboxId=" + event.getId());
            } else {
                // FAILURE
                System.err.println("Kafka send failed for outboxId="
                        + event.getId() + " : " + ex.getMessage());

                // Keep status NEW (auto retry later)
            }
        });
    }
}
