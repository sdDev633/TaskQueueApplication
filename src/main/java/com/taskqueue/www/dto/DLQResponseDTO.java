package com.taskqueue.www.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DLQResponseDTO {
    private Long id;
    private Long originalTaskId;
    private String payload;
    private Integer totalAttempts;
    private String lastError;
    private LocalDateTime failedAt;
    private String status;
    private String resolution;
}