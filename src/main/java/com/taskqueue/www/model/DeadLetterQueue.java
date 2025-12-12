package com.taskqueue.www.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class DeadLetterQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long originalTaskId;

    @Column(length = 5000)
    private String payload;

    private Integer totalAttempts;

    @Column(length = 2000)
    private String lastError;

    private LocalDateTime failedAt = LocalDateTime.now();

    private String status = "FAILED"; // FAILED, RETRYING, RESOLVED

    @Column(length = 1000)
    private String resolution; // Manual resolution notes
}