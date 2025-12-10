package com.taskqueue.www.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TaskHandlerRegistry {

    private final Map<String, TaskHandler> handlers = new HashMap<>();

    public TaskHandlerRegistry(List<TaskHandler> handlerList) {
        for (TaskHandler handler : handlerList) {
            handlers.put(handler.getType(), handler);
            log.info("Registered handler for type: {}", handler.getType());
        }
    }

    public TaskHandler getHandler(String type) {
        return handlers.get(type);
    }

    public boolean hasHandler(String type) {
        return handlers.containsKey(type);
    }
}