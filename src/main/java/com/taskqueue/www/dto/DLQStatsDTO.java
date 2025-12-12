package com.taskqueue.www.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DLQStatsDTO {
    private long totalFailed;
    private long retrying;
    private long resolved;
}