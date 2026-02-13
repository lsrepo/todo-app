package com.pak.todo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.User;
import com.pak.todo.model.enums.TaskStatus;

class TaskControllerTest extends AbstractTaskControllerTest {

	// Scenario: listing tasks when board exists and user can view returns 200
	// Given: Board exists, user can view, TaskService returns a page of tasks
	// When: GET /api/boards/{boardId}/tasks is called
	// Then: the response is 200 OK with content array
	@Test
	void list_boardExistsAndUserCanView_returns200() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		TaskResponse taskResp = TaskResponse.builder()
				.id(UUID.randomUUID())
				.boardId(boardId)
				.name("Task 1")
				.status(TaskStatus.NOT_STARTED)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canViewBoard(user, board)).thenReturn(true);
		when(taskService.findByBoardId(eq(boardId), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(taskResp), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/boards/{boardId}/tasks", boardId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Task 1"));
	}

	// Scenario: list accepts status filter and delegates to service
	// Given: board exists, user can view, service returns empty page
	// When: GET /api/boards/{boardId}/tasks?status=IN_PROGRESS is called
	// Then: response is 200 and findByBoardId was invoked with that status
	@Test
	void list_withStatusFilter_returns200() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canViewBoard(user, board)).thenReturn(true);
		when(taskService.findByBoardId(eq(boardId), eq(TaskStatus.IN_PROGRESS), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		mockMvc.perform(get("/api/boards/{boardId}/tasks", boardId).param("status", "IN_PROGRESS"))
				.andExpect(status().isOk());

		verify(taskService).findByBoardId(eq(boardId), eq(TaskStatus.IN_PROGRESS), any(), any(), any());
	}

	// Scenario: list accepts sort param (framework binds Pageable)
	// Given: board exists, user can view, service returns empty page
	// When: GET /api/boards/{boardId}/tasks?sort=dueDate is called
	// Then: response is 200
	@Test
	void list_withSortParam_returns200() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canViewBoard(user, board)).thenReturn(true);
		when(taskService.findByBoardId(eq(boardId), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		mockMvc.perform(get("/api/boards/{boardId}/tasks", boardId).param("sort", "dueDate"))
				.andExpect(status().isOk());
	}

	// Scenario: listing tasks when board does not exist returns 404
	// Given: BoardService returns null for board id
	// When: GET /api/boards/{boardId}/tasks is called
	// Then: the response is 404 Not Found
	@Test
	void list_boardNotFound_returns404() throws Exception {
		UUID boardId = UUID.randomUUID();
		when(boardService.getEntityById(boardId)).thenReturn(null);

		mockMvc.perform(get("/api/boards/{boardId}/tasks", boardId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	// Scenario: listing tasks when user cannot view board returns 403
	// Given: Board exists but canViewBoard returns false
	// When: GET /api/boards/{boardId}/tasks is called
	// Then: the response is 403 Forbidden
	@Test
	void list_userCannotView_returns403() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canViewBoard(user, board)).thenReturn(false);

		mockMvc.perform(get("/api/boards/{boardId}/tasks", boardId))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	// Scenario: getting a task when the board does not exist
	// Given: BoardService returns null for the given board id
	// When: GET /api/boards/{boardId}/tasks/{taskId} is called
	// Then: the response is 404 Not Found with NOT_FOUND code
	@Test
	void get_boardNotFound_returns404() throws Exception {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		when(boardService.getEntityById(boardId)).thenReturn(null);

		mockMvc.perform(get("/api/boards/{boardId}/tasks/{taskId}", boardId, taskId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Board not found: " + boardId));
	}

	// Scenario: getting a task when task exists returns 200 and body
	// Given: Board exists, user can view, TaskService returns task
	// When: GET /api/boards/{boardId}/tasks/{taskId} is called
	// Then: the response is 200 OK with task body
	@Test
	void get_taskExists_returns200AndBody() throws Exception {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		TaskResponse response = TaskResponse.builder()
				.id(taskId)
				.boardId(boardId)
				.name("Task")
				.status(TaskStatus.IN_PROGRESS)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canViewBoard(user, board)).thenReturn(true);
		when(taskService.findByBoardIdAndTaskId(boardId, taskId)).thenReturn(response);

		mockMvc.perform(get("/api/boards/{boardId}/tasks/{taskId}", boardId, taskId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(taskId.toString()))
				.andExpect(jsonPath("$.name").value("Task"));
	}

	// Scenario: getting a task when task does not exist returns 404
	// Given: Board exists, user can view, TaskService returns null
	// When: GET /api/boards/{boardId}/tasks/{taskId} is called
	// Then: the response is 404 Not Found
	@Test
	void get_taskNotFound_returns404() throws Exception {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canViewBoard(user, board)).thenReturn(true);
		when(taskService.findByBoardIdAndTaskId(boardId, taskId)).thenReturn(null);

		mockMvc.perform(get("/api/boards/{boardId}/tasks/{taskId}", boardId, taskId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Task not found: " + taskId));
	}

	// Scenario: creating a task with valid request returns 201 and body
	// Given: Board exists, user can modify, create handler returns TaskResponse
	// When: POST /api/boards/{boardId}/tasks with valid JSON is called
	// Then: the response is 201 Created with task body
	@Test
	void create_validRequest_returns201() throws Exception {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		TaskResponse response = TaskResponse.builder()
				.id(taskId)
				.boardId(boardId)
				.name("New Task")
				.description("Desc")
				.status(TaskStatus.NOT_STARTED)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canModifyTasks(user, board)).thenReturn(true);
		when(createTaskCommandHandler.handle(any())).thenReturn(response);

		String json = "{\"name\":\"New Task\",\"description\":\"Desc\"}";
		mockMvc.perform(post("/api/boards/{boardId}/tasks", boardId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(taskId.toString()))
				.andExpect(jsonPath("$.name").value("New Task"));
	}

	// Scenario: creating a task when board does not exist returns 404
	// Given: BoardService returns null
	// When: POST /api/boards/{boardId}/tasks is called
	// Then: the response is 404 Not Found
	@Test
	void create_boardNotFound_returns404() throws Exception {
		UUID boardId = UUID.randomUUID();
		when(boardService.getEntityById(boardId)).thenReturn(null);

		mockMvc.perform(post("/api/boards/{boardId}/tasks", boardId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Task\",\"description\":\"\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	// Scenario: creating a task when user cannot modify returns 403
	// Given: Board exists but canModifyTasks returns false
	// When: POST /api/boards/{boardId}/tasks is called
	// Then: the response is 403 Forbidden
	@Test
	void create_userCannotModify_returns403() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canModifyTasks(user, board)).thenReturn(false);

		mockMvc.perform(post("/api/boards/{boardId}/tasks", boardId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Task\",\"description\":\"\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	// Scenario: updating a task when task not found returns 404
	// Given: Board exists, user can modify, update handler returns null
	// When: PUT /api/boards/{boardId}/tasks/{taskId} is called
	// Then: the response is 404 Not Found
	@Test
	void update_taskNotFound_returns404() throws Exception {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canModifyTasks(user, board)).thenReturn(true);
		when(updateTaskCommandHandler.handle(any())).thenReturn(null);

		mockMvc.perform(put("/api/boards/{boardId}/tasks/{taskId}", boardId, taskId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Updated\",\"description\":\"\",\"status\":\"IN_PROGRESS\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	// Scenario: updating a task when user cannot modify returns 403
	// Given: Board exists but canModifyTasks returns false
	// When: PUT /api/boards/{boardId}/tasks/{taskId} is called
	// Then: the response is 403 Forbidden
	@Test
	void update_userCannotModify_returns403() throws Exception {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canModifyTasks(user, board)).thenReturn(false);

		mockMvc.perform(put("/api/boards/{boardId}/tasks/{taskId}", boardId, taskId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Updated\",\"description\":\"\",\"status\":\"IN_PROGRESS\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	// Scenario: updating a task successfully returns 200 and body
	// Given: Board exists, user can modify, update handler returns TaskResponse
	// When: PUT /api/boards/{boardId}/tasks/{taskId} is called
	// Then: the response is 200 OK with task body
	@Test
	void update_success_returns200() throws Exception {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		TaskResponse response = TaskResponse.builder()
				.id(taskId)
				.boardId(boardId)
				.name("Updated Task")
				.status(TaskStatus.COMPLETED)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canModifyTasks(user, board)).thenReturn(true);
		when(updateTaskCommandHandler.handle(any())).thenReturn(response);

		mockMvc.perform(put("/api/boards/{boardId}/tasks/{taskId}", boardId, taskId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Updated Task\",\"description\":\"\",\"status\":\"COMPLETED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Task"))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	// Scenario: deleting a task when task not found returns 404
	// Given: Board exists, user can modify, delete handler returns false
	// When: DELETE /api/boards/{boardId}/tasks/{taskId} is called
	// Then: the response is 404 Not Found
	@Test
	void delete_taskNotFound_returns404() throws Exception {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canModifyTasks(user, board)).thenReturn(true);
		when(deleteTaskCommandHandler.handle(boardId, taskId)).thenReturn(false);

		mockMvc.perform(delete("/api/boards/{boardId}/tasks/{taskId}", boardId, taskId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	// Scenario: deleting a task successfully returns 204
	// Given: Board exists, user can modify, delete handler returns true
	// When: DELETE /api/boards/{boardId}/tasks/{taskId} is called
	// Then: the response is 204 No Content
	@Test
	void delete_success_returns204() throws Exception {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canModifyTasks(user, board)).thenReturn(true);
		when(deleteTaskCommandHandler.handle(boardId, taskId)).thenReturn(true);

		mockMvc.perform(delete("/api/boards/{boardId}/tasks/{taskId}", boardId, taskId))
				.andExpect(status().isNoContent());

		verify(deleteTaskCommandHandler).handle(boardId, taskId);
	}
}
