package com.taskqueue.www.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCreateRequest {
    private String type;          // EMAIL, PDF, WEBHOOK, etc
    private JsonNode data;        // dynamic payload
    private JsonNode meta;        // optional
}
