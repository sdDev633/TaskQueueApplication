package com.taskqueue.www.repository;

import com.taskqueue.www.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatus(String status);
    Optional<OutboxEvent> findTopByTaskIdOrderByCreatedAtDesc(Long taskId);

}