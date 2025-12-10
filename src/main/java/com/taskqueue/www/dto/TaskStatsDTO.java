package com.taskqueue.www.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatsDTO {
    private long total;
    private long pending;
    private long processing;
    private long done;
    private long failed;
}