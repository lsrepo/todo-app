package com.pak.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Task;
import com.pak.todo.model.enums.TaskStatus;
import com.pak.todo.model.mapper.TaskMapper;
import com.pak.todo.repository.TaskRepository;

class TaskServiceTest {

	// Scenario: findByBoardId returns a paginated list of task responses
	// Given: the repository returns a page of tasks for the board
	// When: findByBoardId is called with boardId and pageable
	// Then: the mapper is applied to each task and a page of TaskResponse is returned
	@Test
	void findByBoardId_returnsPageOfTaskResponse() {
		TaskRepository taskRepository = mock(TaskRepository.class);
		TaskMapper taskMapper = mock(TaskMapper.class);
		UUID boardId = UUID.randomUUID();
		Board board = Board.create(boardId, "Board", null);
		Task task = Task.create(UUID.randomUUID(), board, "Task", "Desc", Instant.now(), TaskStatus.NOT_STARTED);
		TaskResponse response = TaskResponse.builder()
				.id(task.getId())
				.boardId(boardId)
				.name(task.getName())
				.build();
		PageImpl<Task> page = new PageImpl<>(List.of(task));
		when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
		when(taskMapper.toResponse(task)).thenReturn(response);

		TaskService service = new TaskService(taskRepository, taskMapper);
		var result = service.findByBoardId(boardId, null, null, null, org.springframework.data.domain.PageRequest.of(0, 20));

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0)).isSameAs(response);
		verify(taskMapper).toResponse(task);
	}

	// Scenario: findByBoardIdAndTaskId returns the mapped response when task exists
	// Given: the repository has a task for the given board and task id
	// When: findByBoardIdAndTaskId(boardId, taskId) is called
	// Then: the BoardResponse is returned
	@Test
	void findByBoardIdAndTaskId_existingTask_returnsTaskResponse() {
		TaskRepository taskRepository = mock(TaskRepository.class);
		TaskMapper taskMapper = mock(TaskMapper.class);
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Board board = Board.create(boardId, "Board", null);
		Task task = Task.create(taskId, board, "Task", "Desc", Instant.now(), TaskStatus.NOT_STARTED);
		TaskResponse response = TaskResponse.builder().id(taskId).boardId(boardId).name("Task").build();
		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.of(task));
		when(taskMapper.toResponse(task)).thenReturn(response);

		TaskService service = new TaskService(taskRepository, taskMapper);
		TaskResponse result = service.findByBoardIdAndTaskId(boardId, taskId);

		assertThat(result).isSameAs(response);
		verify(taskMapper).toResponse(task);
	}

	// Scenario: findByBoardIdAndTaskId returns null when task does not exist
	// Given: the repository has no task for the given board and task id
	// When: findByBoardIdAndTaskId is called
	// Then: null is returned
	@Test
	void findByBoardIdAndTaskId_missingTask_returnsNull() {
		TaskRepository taskRepository = mock(TaskRepository.class);
		TaskMapper taskMapper = mock(TaskMapper.class);
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.empty());

		TaskService service = new TaskService(taskRepository, taskMapper);
		TaskResponse result = service.findByBoardIdAndTaskId(boardId, taskId);

		assertThat(result).isNull();
		verify(taskMapper, org.mockito.Mockito.never()).toResponse(any());
	}

	// Scenario: getEntityByBoardIdAndTaskId returns the entity when it exists
	// Given: the repository has a task for the given board and task id
	// When: getEntityByBoardIdAndTaskId is called
	// Then: the Task entity is returned
	@Test
	void getEntityByBoardIdAndTaskId_existingTask_returnsTask() {
		TaskRepository taskRepository = mock(TaskRepository.class);
		TaskMapper taskMapper = mock(TaskMapper.class);
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Board board = Board.create(boardId, "Board", null);
		Task task = Task.create(taskId, board, "Task", "Desc", Instant.now(), TaskStatus.NOT_STARTED);
		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.of(task));

		TaskService service = new TaskService(taskRepository, taskMapper);
		Task result = service.getEntityByBoardIdAndTaskId(boardId, taskId);

		assertThat(result).isSameAs(task);
	}

	// Scenario: getEntityByBoardIdAndTaskId returns null when task does not exist
	// Given: the repository has no task for the given ids
	// When: getEntityByBoardIdAndTaskId is called
	// Then: null is returned
	@Test
	void getEntityByBoardIdAndTaskId_missingTask_returnsNull() {
		TaskRepository taskRepository = mock(TaskRepository.class);
		TaskMapper taskMapper = mock(TaskMapper.class);
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.empty());

		TaskService service = new TaskService(taskRepository, taskMapper);
		Task result = service.getEntityByBoardIdAndTaskId(boardId, taskId);

		assertThat(result).isNull();
	}
}
