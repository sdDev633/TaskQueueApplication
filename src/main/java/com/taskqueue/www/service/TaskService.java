package com.taskqueue.www.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskqueue.www.dto.OutboxStatusDTO;
import com.taskqueue.www.dto.TaskCreateRequestDTO;
import com.taskqueue.www.dto.TaskResponseDTO;
import com.taskqueue.www.dto.TaskStatsDTO;
import com.taskqueue.www.security.CustomUserDetails;
import com.taskqueue.www.security.SecurityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import com.taskqueue.www.model.OutboxEvent;
import com.taskqueue.www.model.Task;
import com.taskqueue.www.repository.OutboxRepository;
import com.taskqueue.www.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* ================= CREATE ================= */

    public TaskResponseDTO createTask(TaskCreateRequestDTO request) {

        Task task = new Task();
        task.setPayload(request.getPayload());
        task.setStatus("PENDING");
        task.setUserId(SecurityUtils.currentUserId());

        Task saved = taskRepository.save(task);
        OutboxEvent savedEvent = outboxRepository.save(createOutbox(saved));

        return mapToDTO(saved, savedEvent);
    }

    /* ================= READ ================= */

    public Page<TaskResponseDTO> getAllTasks(Pageable pageable) {

        Page<Task> page = SecurityUtils.isAdmin()
                ? taskRepository.findAll(pageable)
                : taskRepository.findByUserId(SecurityUtils.currentUserId(), pageable);

        return page.map(t -> mapToDTO(t, findOutboxForTask(t.getId())));
    }

    public Optional<TaskResponseDTO> getTaskById(Long id) {

        Optional<Task> task = SecurityUtils.isAdmin()
                ? taskRepository.findById(id)
                : taskRepository.findByIdAndUserId(id, SecurityUtils.currentUserId());

        return task.map(t -> mapToDTO(t, findOutboxForTask(t.getId())));
    }

    public Optional<String> getTaskStatus(Long id) {

        Optional<Task> task = SecurityUtils.isAdmin()
                ? taskRepository.findById(id)
                : taskRepository.findByIdAndUserId(id, SecurityUtils.currentUserId());

        return task.map(Task::getStatus);
    }

    public Page<TaskResponseDTO> getTasksByStatus(String status, Pageable pageable) {

        Page<Task> page = SecurityUtils.isAdmin()
                ? taskRepository.findByStatus(status, pageable)
                : taskRepository.findByStatusAndUserId(
                status, SecurityUtils.currentUserId(), pageable);

        return page.map(t -> mapToDTO(t, findOutboxForTask(t.getId())));
    }

    public TaskStatsDTO getStats() {

        if (SecurityUtils.isAdmin()) {
            return new TaskStatsDTO(
                    taskRepository.count(),
                    taskRepository.countByStatus("PENDING"),
                    taskRepository.countByStatus("PROCESSING"),
                    taskRepository.countByStatus("DONE"),
                    taskRepository.countByStatus("FAILED")
            );
        }

        Long uid = SecurityUtils.currentUserId();
        return new TaskStatsDTO(
                taskRepository.countByUserId(uid),
                taskRepository.countByStatusAndUserId("PENDING", uid),
                taskRepository.countByStatusAndUserId("PROCESSING", uid),
                taskRepository.countByStatusAndUserId("DONE", uid),
                taskRepository.countByStatusAndUserId("FAILED", uid)
        );
    }

    /* ================= MUTATIONS ================= */

    @Transactional
    public Optional<TaskResponseDTO> cancelTask(Long id) {

        return findAuthorizedTask(id).map(task -> {
            if ("PENDING".equals(task.getStatus()) || "PROCESSING".equals(task.getStatus())) {
                task.setStatus("CANCELLED");
                taskRepository.save(task);
            }
            return mapToDTO(task, findOutboxForTask(task.getId()));
        });
    }

    @Transactional
    public Optional<TaskResponseDTO> retryTask(Long id) {

        return findAuthorizedTask(id).map(task -> {

            if (!"FAILED".equals(task.getStatus()) && !"CANCELLED".equals(task.getStatus())) {
                return mapToDTO(task, findOutboxForTask(task.getId()));
            }

            task.setStatus("PENDING");
            Task saved = taskRepository.save(task);

            OutboxEvent event = outboxRepository.save(createOutbox(saved));
            return mapToDTO(saved, event);
        });
    }

    @Transactional
    public boolean deleteTask(Long id) {

        return findAuthorizedTask(id).map(t -> {
            taskRepository.delete(t);
            return true;
        }).orElse(false);
    }

    /* ================= INTERNAL ================= */

    private Optional<Task> findAuthorizedTask(Long id) {
        return SecurityUtils.isAdmin()
                ? taskRepository.findById(id)
                : taskRepository.findByIdAndUserId(id, SecurityUtils.currentUserId());
    }

    private OutboxEvent createOutbox(Task task) {

        ObjectNode node = objectMapper.createObjectNode();
        node.put("taskId", task.getId());
        node.put("payload", task.getPayload());

        OutboxEvent event = new OutboxEvent();
        event.setTaskId(task.getId());
        event.setPayload(node.toString());
        event.setStatus("NEW");
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private OutboxEvent findOutboxForTask(Long taskId) {
        return outboxRepository
                .findTopByTaskIdOrderByCreatedAtDesc(taskId)
                .orElse(null);
    }

    private TaskResponseDTO mapToDTO(Task task, OutboxEvent outbox) {

        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setId(task.getId());
        dto.setPayload(task.getPayload());
        dto.setStatus(task.getStatus());

        if (outbox != null) {
            OutboxStatusDTO o = new OutboxStatusDTO();
            o.setStatus(outbox.getStatus());
            o.setCreatedAt(outbox.getCreatedAt());
            dto.setOutboxStatus(o);
        }
        return dto;
    }
}
