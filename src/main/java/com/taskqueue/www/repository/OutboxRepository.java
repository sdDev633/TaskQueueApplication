package com.taskqueue.www.repository;

import com.taskqueue.www.model.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatus(String status);
    Optional<OutboxEvent> findTopByTaskIdOrderByCreatedAtDesc(Long taskId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);


}