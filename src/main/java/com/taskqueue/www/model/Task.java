package com.taskqueue.www.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String payload;

    private String status; // PENDING, PROCESSING, DONE, FAILED, CANCELLED

    private Integer retryCount = 0;

    private Integer maxRetries = 3;

    @Column(length = 1000)
    private String errorMessage;

    private LocalDateTime lastAttemptAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    private Long retriedFromDlqId; // Track if this task was retried from DLQ

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}