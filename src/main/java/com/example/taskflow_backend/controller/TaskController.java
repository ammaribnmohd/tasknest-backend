package com.example.taskflow_backend.controller;

import com.example.taskflow_backend.dto.TaskRequest;
import com.example.taskflow_backend.dto.TaskResponse;
import com.example.taskflow_backend.enums.TaskStatus;
import com.example.taskflow_backend.security.AppUserDetails;
import com.example.taskflow_backend.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Endpoints for managing tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "Get all tasks for current user")
    public ResponseEntity<List<TaskResponse>> getAllTasks(@AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.ok(taskService.getAllTasks(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by ID for current user")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable String id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        return ResponseEntity.ok(taskService.getTaskById(id, principal.getId()));
    }

    @PostMapping
    @Operation(summary = "Create a new task for current user")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        TaskResponse createdTask = taskService.createTask(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing task for current user")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable String id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, request, principal.getId()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update only the status of a task for current user")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable String id,
            @RequestParam TaskStatus status,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, status, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task by ID for current user")
    public ResponseEntity<Void> deleteTask(
            @PathVariable String id,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        taskService.deleteTask(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
