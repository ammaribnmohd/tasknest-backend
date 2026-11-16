package com.example.taskflow_backend.repository;

import com.example.taskflow_backend.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Every query is scoped by ownerId. There is intentionally no unscoped findAll/findById
 * usage in the service layer, so one user can never read another user's tasks.
 */
@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByOwnerId(String ownerId);
    Optional<Task> findByIdAndOwnerId(String id, String ownerId);
    boolean existsByIdAndOwnerId(String id, String ownerId);
    void deleteByIdAndOwnerId(String id, String ownerId);
}
