package com.pak.todo.web;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pak.todo.auth.AuthorizationService;
import com.pak.todo.command.CreateTaskCommandHandler;
import com.pak.todo.command.DeleteTaskCommandHandler;
import com.pak.todo.command.UpdateTaskCommandHandler;
import com.pak.todo.domain.command.CreateTaskCommand;
import com.pak.todo.domain.command.UpdateTaskCommand;
import com.pak.todo.model.dto.TaskCreateRequest;
import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.dto.TaskUpdateRequest;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.User;
import com.pak.todo.model.enums.TaskStatus;
import com.pak.todo.security.CurrentUserService;
import com.pak.todo.service.BoardService;
import com.pak.todo.service.TaskService;
import com.pak.todo.web.error.ResourceNotFoundException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/boards/{boardId}/tasks")
@RequiredArgsConstructor
public class TaskController {

	private final TaskService taskService;
	private final CreateTaskCommandHandler createTaskCommandHandler;
	private final UpdateTaskCommandHandler updateTaskCommandHandler;
	private final DeleteTaskCommandHandler deleteTaskCommandHandler;
	private final BoardService boardService;
	private final AuthorizationService authorizationService;
	private final CurrentUserService currentUserService;

	@GetMapping
	public Page<TaskResponse> list(
			@PathVariable UUID boardId,
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueTo,
			@PageableDefault(size = 20, sort = "dueDate") Pageable pageable
	) {
		Board board = boardService.getEntityById(boardId);
		if (board == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}

		User currentUser = currentUserService.getCurrentUserOrThrow();
		if (!authorizationService.canViewBoard(currentUser, board)) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied to board " + boardId);
		}

		return taskService.findByBoardId(boardId, status, dueFrom, dueTo, pageable);
	}

	@GetMapping("/{taskId}")
	public TaskResponse get(@PathVariable UUID boardId, @PathVariable UUID taskId) {
		Board board = boardService.getEntityById(boardId);
		if (board == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}

		User currentUser = currentUserService.getCurrentUserOrThrow();
		if (!authorizationService.canViewBoard(currentUser, board)) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied to board " + boardId);
		}

		TaskResponse response = taskService.findByBoardIdAndTaskId(boardId, taskId);
		if (response == null) {
			throw new ResourceNotFoundException("Task not found: " + taskId);
		}
		return response;
	}

	@PostMapping
	public ResponseEntity<TaskResponse> create(
			@PathVariable UUID boardId,
			@Valid @RequestBody TaskCreateRequest request
	) {
		Board board = boardService.getEntityById(boardId);
		if (board == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}

		User currentUser = currentUserService.getCurrentUserOrThrow();
		if (!authorizationService.canModifyTasks(currentUser, board)) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied to board " + boardId);
		}

		CreateTaskCommand command = CreateTaskCommand.from(boardId, request);
		TaskResponse response = createTaskCommandHandler.handle(command);
		if (response == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{taskId}")
	public TaskResponse update(
			@PathVariable UUID boardId,
			@PathVariable UUID taskId,
			@Valid @RequestBody TaskUpdateRequest request
	) {
		Board board = boardService.getEntityById(boardId);
		if (board == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}

		User currentUser = currentUserService.getCurrentUserOrThrow();
		if (!authorizationService.canModifyTasks(currentUser, board)) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied to board " + boardId);
		}

		UpdateTaskCommand command = UpdateTaskCommand.from(boardId, taskId, request);
		TaskResponse response = updateTaskCommandHandler.handle(command);
		if (response == null) {
			throw new ResourceNotFoundException("Task not found: " + taskId);
		}
		return response;
	}

	@DeleteMapping("/{taskId}")
	public ResponseEntity<Void> delete(@PathVariable UUID boardId, @PathVariable UUID taskId) {
		Board board = boardService.getEntityById(boardId);
		if (board == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}

		User currentUser = currentUserService.getCurrentUserOrThrow();
		if (!authorizationService.canModifyTasks(currentUser, board)) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied to board " + boardId);
		}

		boolean deleted = deleteTaskCommandHandler.handle(boardId, taskId);
		if (!deleted) {
			throw new ResourceNotFoundException("Task not found: " + taskId);
		}
		return ResponseEntity.noContent().build();
	}
}
