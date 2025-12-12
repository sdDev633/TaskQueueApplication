package com.taskqueue.www.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkRetryResponseDTO {
    private int totalRetried;
    private int successful;
    private int failed;
    private String message;
}