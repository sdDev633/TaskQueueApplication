package com.taskqueue.www.controller;

import com.taskqueue.www.model.Task;
import com.taskqueue.www.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public String createTask(@RequestBody Task task) {
        return taskService.enqueue(task);
    }
}