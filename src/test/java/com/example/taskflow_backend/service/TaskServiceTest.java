package com.example.taskflow_backend.service;

import com.example.taskflow_backend.dto.TaskRequest;
import com.example.taskflow_backend.dto.TaskResponse;
import com.example.taskflow_backend.enums.TaskPriority;
import com.example.taskflow_backend.enums.TaskStatus;
import com.example.taskflow_backend.exception.ResourceNotFoundException;
import com.example.taskflow_backend.model.Task;
import com.example.taskflow_backend.repository.TaskRepository;
import com.example.taskflow_backend.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(taskRepository);
    }

    @Test
    @DisplayName("getAllTasks queries only by ownerId")
    void getAllTasksScopedToOwner() {
        String ownerId = "user-1";
        Task task = new Task(ownerId, "Title", "Desc", TaskStatus.TODO, TaskPriority.HIGH, null);
        task.setId("t-1");

        when(taskRepository.findByOwnerId(ownerId)).thenReturn(List.of(task));

        List<TaskResponse> list = taskService.getAllTasks(ownerId);
        assertEquals(1, list.size());
        assertEquals("Title", list.get(0).title());
        verify(taskRepository).findByOwnerId(ownerId);
    }

    @Test
    @DisplayName("getTaskById with wrong ownerId throws ResourceNotFoundException")
    void getTaskByIdUnauthorizedOwner() {
        when(taskRepository.findByIdAndOwnerId("t-1", "user-2")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById("t-1", "user-2"));
        verify(taskRepository).findByIdAndOwnerId("t-1", "user-2");
    }

    @Test
    @DisplayName("createTask attaches ownerId to new task")
    void createTaskAttachesOwner() {
        String ownerId = "user-1";
        TaskRequest req = new TaskRequest("New task", "Details", TaskStatus.TODO, TaskPriority.MEDIUM, null);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId("new-id");
            return t;
        });

        TaskResponse res = taskService.createTask(req, ownerId);
        assertNotNull(res);
        assertEquals("New task", res.title());
        verify(taskRepository).save(argThat(task -> ownerId.equals(task.getOwnerId())));
    }
}

