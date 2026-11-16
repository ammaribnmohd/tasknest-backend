package com.example.taskflow_backend.dto;

import com.example.taskflow_backend.enums.TaskPriority;
import com.example.taskflow_backend.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record TaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        TaskStatus status,
        TaskPriority priority,
        Instant dueDate
) {}

