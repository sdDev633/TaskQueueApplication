package com.taskqueue.www.repository;

import com.taskqueue.www.model.DeadLetterQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, Long> {

    Page<DeadLetterQueue> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}