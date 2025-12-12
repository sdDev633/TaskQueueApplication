package com.taskqueue.www.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePayloadRequestDTO {
    private String payload; // New corrected payload
    private String resolution; // Note about what was fixed
}
