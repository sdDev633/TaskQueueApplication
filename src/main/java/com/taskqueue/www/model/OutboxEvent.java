package com.taskqueue.www.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Data
@Getter
@Setter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;   // ✅ ADD THIS

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}