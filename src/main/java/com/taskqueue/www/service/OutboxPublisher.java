package com.taskqueue.www.service;

import com.taskqueue.www.kafka.KafkaProducerService;
import com.taskqueue.www.model.OutboxEvent;
import com.taskqueue.www.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaProducerService producer;

    @Scheduled(fixedRate = 5000) // every 5 seconds
    public void publishOutboxEvents() {

        List<OutboxEvent> events = outboxRepository.findByStatus("NEW");

        for (OutboxEvent event : events) {
            producer.sendTask(event.getPayload());
            event.setStatus("SENT");
            outboxRepository.save(event);
        }
    }
}
