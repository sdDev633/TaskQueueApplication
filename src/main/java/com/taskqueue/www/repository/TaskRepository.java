package com.taskqueue.www.repository;

import com.taskqueue.www.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}