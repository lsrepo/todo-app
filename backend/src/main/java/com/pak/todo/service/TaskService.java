package com.pak.todo.service;

import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.entity.Task;
import com.pak.todo.model.enums.TaskStatus;
import com.pak.todo.repository.TaskRepository;
import com.pak.todo.model.mapper.TaskMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

	private final TaskRepository taskRepository;
	private final TaskMapper taskMapper;

	@Transactional(readOnly = true)
	public Page<TaskResponse> findByBoardId(UUID boardId, TaskStatus status, Instant dueFrom, Instant dueTo, Pageable pageable) {
		Specification<Task> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("board").get("id"), boardId));
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (dueFrom != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueFrom));
			}
			if (dueTo != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueTo));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		return taskRepository.findAll(spec, pageable).map(taskMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public TaskResponse findByBoardIdAndTaskId(UUID boardId, UUID taskId) {
		return taskRepository.findByIdAndBoardId(taskId, boardId)
				.map(taskMapper::toResponse)
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public Task getEntityByBoardIdAndTaskId(UUID boardId, UUID taskId) {
		return taskRepository.findByIdAndBoardId(taskId, boardId).orElse(null);
	}
}
