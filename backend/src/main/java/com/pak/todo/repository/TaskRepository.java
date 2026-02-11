package com.pak.todo.repository;

import com.pak.todo.model.entity.Task;
import com.pak.todo.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

	Optional<Task> findByIdAndBoardId(UUID taskId, UUID boardId);

	Page<Task> findAllByBoardId(UUID boardId, Pageable pageable);

	boolean existsByIdAndBoardId(UUID taskId, UUID boardId);
}
