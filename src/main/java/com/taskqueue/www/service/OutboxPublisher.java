package com.taskqueue.www.service;

import com.taskqueue.www.kafka.KafkaProducerService;
import com.taskqueue.www.model.OutboxEvent;
import com.taskqueue.www.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaProducerService producer;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void publishOutboxEvents() {

        List<OutboxEvent> events =
                outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("NEW");

        for (OutboxEvent event : events) {
            event.setStatus("PROCESSING");
        }

        outboxRepository.saveAll(events);

        events.forEach(producer::sendTask);
    }
}

