package com.example.taskflow_backend.service.impl;

import com.example.taskflow_backend.dto.TaskRequest;
import com.example.taskflow_backend.dto.TaskResponse;
import com.example.taskflow_backend.enums.TaskStatus;
import com.example.taskflow_backend.exception.ResourceNotFoundException;
import com.example.taskflow_backend.model.Task;
import com.example.taskflow_backend.repository.TaskRepository;
import com.example.taskflow_backend.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public List<TaskResponse> getAllTasks(String ownerId) {
        return taskRepository.findByOwnerId(ownerId)
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Override
    public TaskResponse getTaskById(String id, String ownerId) {
        Task task = findEntityByIdAndOwnerId(id, ownerId);
        return TaskResponse.fromEntity(task);
    }

    @Override
    public TaskResponse createTask(TaskRequest request, String ownerId) {
        Task task = new Task(
                ownerId,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueDate()
        );
        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    @Override
    public TaskResponse updateTask(String id, TaskRequest request, String ownerId) {
        Task task = findEntityByIdAndOwnerId(id, ownerId);
        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDueDate(request.dueDate());
        task.setUpdatedAt(Instant.now());

        Task updated = taskRepository.save(task);
        return TaskResponse.fromEntity(updated);
    }

    @Override
    public TaskResponse updateTaskStatus(String id, TaskStatus status, String ownerId) {
        Task task = findEntityByIdAndOwnerId(id, ownerId);
        task.setStatus(status);
        task.setUpdatedAt(Instant.now());

        Task updated = taskRepository.save(task);
        return TaskResponse.fromEntity(updated);
    }

    @Override
    public void deleteTask(String id, String ownerId) {
        if (!taskRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new ResourceNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private Task findEntityByIdAndOwnerId(String id, String ownerId) {
        return taskRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }
}
