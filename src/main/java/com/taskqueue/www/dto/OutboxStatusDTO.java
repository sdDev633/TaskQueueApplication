package com.taskqueue.www.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutboxStatusDTO {
    private String status;
    private LocalDateTime createdAt;
}