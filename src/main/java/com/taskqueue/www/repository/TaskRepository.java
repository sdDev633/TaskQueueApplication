package com.taskqueue.www.repository;

import com.taskqueue.www.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByUserId(Long userId, Pageable pageable);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    Page<Task> findByStatusAndUserId(String status, Long userId, Pageable pageable);

    Page<Task> findByStatus(String status, Pageable pageable);

    long countByStatusAndUserId(String status, Long userId);

    long countByUserId(Long userId);

    long countByStatus(String status);
}

