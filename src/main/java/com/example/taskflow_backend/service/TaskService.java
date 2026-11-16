package com.example.taskflow_backend.service;

import com.example.taskflow_backend.dto.TaskRequest;
import com.example.taskflow_backend.dto.TaskResponse;
import com.example.taskflow_backend.enums.TaskStatus;

import java.util.List;

public interface TaskService {
    List<TaskResponse> getAllTasks(String ownerId);
    TaskResponse getTaskById(String id, String ownerId);
    TaskResponse createTask(TaskRequest request, String ownerId);
    TaskResponse updateTask(String id, TaskRequest request, String ownerId);
    TaskResponse updateTaskStatus(String id, TaskStatus status, String ownerId);
    void deleteTask(String id, String ownerId);
}
