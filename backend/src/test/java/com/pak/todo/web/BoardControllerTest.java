package com.pak.todo.web;

import static org.mockito.ArgumentMatchers.any;
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

import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.User;

class BoardControllerTest extends AbstractBoardControllerTest {

	// Scenario: listing boards returns paginated result
	// Given: BoardService returns a page of boards
	// When: GET /api/boards is called
	// Then: the response is 200 OK with content array
	@Test
	void list_returnsPaginatedBoards() throws Exception {
		BoardResponse response = BoardResponse.builder()
				.id(UUID.randomUUID())
				.name("Board 1")
				.description("Desc")
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		when(boardService.findAll(any())).thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/boards"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(response.getId().toString()))
				.andExpect(jsonPath("$.content[0].name").value("Board 1"));
	}

	// Scenario: getting a board by id when the board does not exist
	// Given: BoardService returns null for the given board id
	// When: GET /api/boards/{boardId} is called
	// Then: the response is 404 Not Found with NOT_FOUND code and message
	@Test
	void get_boardNotFound_returns404() throws Exception {
		UUID boardId = UUID.randomUUID();
		when(boardService.getEntityById(boardId)).thenReturn(null);

		mockMvc.perform(get("/api/boards/{boardId}", boardId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Board not found: " + boardId));
	}

	// Scenario: getting a board when board exists and user can view
	// Given: BoardService returns board and findById returns response, user can view
	// When: GET /api/boards/{boardId} is called
	// Then: the response is 200 OK with board body
	@Test
	void get_boardExistsAndUserCanView_returns200AndBody() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		BoardResponse response = BoardResponse.builder()
				.id(boardId)
				.name("Board")
				.description("Desc")
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canViewBoard(user, board)).thenReturn(true);
		when(boardService.findById(boardId)).thenReturn(response);

		mockMvc.perform(get("/api/boards/{boardId}", boardId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(boardId.toString()))
				.andExpect(jsonPath("$.name").value("Board"));
	}

	// Scenario: getting a board when user cannot view returns 403
	// Given: Board exists but authorizationService.canViewBoard returns false
	// When: GET /api/boards/{boardId} is called
	// Then: the response is 403 Forbidden with FORBIDDEN code
	@Test
	void get_userCannotView_returns403() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canViewBoard(user, board)).thenReturn(false);

		mockMvc.perform(get("/api/boards/{boardId}", boardId))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	// Scenario: creating a board with valid request returns 201 and body
	// Given: BoardCreationService returns BoardResponse for the request
	// When: POST /api/boards with valid JSON is called
	// Then: the response is 201 Created with board body
	@Test
	void create_validRequest_returns201AndBody() throws Exception {
		UUID boardId = UUID.randomUUID();
		BoardResponse response = BoardResponse.builder()
				.id(boardId)
				.name("New Board")
				.description("Desc")
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		when(boardCreationService.createBoardWithOwner(any())).thenReturn(response);

		mockMvc.perform(post("/api/boards")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"New Board\",\"description\":\"Desc\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(boardId.toString()))
				.andExpect(jsonPath("$.name").value("New Board"));
	}

	// Scenario: updating a board when board does not exist returns 404
	// Given: BoardService.getEntityById returns null
	// When: PUT /api/boards/{boardId} is called
	// Then: the response is 404 Not Found
	@Test
	void update_boardNotFound_returns404() throws Exception {
		UUID boardId = UUID.randomUUID();
		when(boardService.getEntityById(boardId)).thenReturn(null);

		mockMvc.perform(put("/api/boards/{boardId}", boardId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Updated\",\"description\":\"\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	// Scenario: updating a board when user cannot edit returns 403
	// Given: Board exists but authorizationService.canEditBoard returns false
	// When: PUT /api/boards/{boardId} is called
	// Then: the response is 403 Forbidden
	@Test
	void update_userCannotEdit_returns403() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canEditBoard(user, board)).thenReturn(false);

		mockMvc.perform(put("/api/boards/{boardId}", boardId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Updated\",\"description\":\"\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	// Scenario: deleting a board when board does not exist returns 404
	// Given: BoardService.getEntityById returns null
	// When: DELETE /api/boards/{boardId} is called
	// Then: the response is 404 Not Found
	@Test
	void delete_boardNotFound_returns404() throws Exception {
		UUID boardId = UUID.randomUUID();
		when(boardService.getEntityById(boardId)).thenReturn(null);

		mockMvc.perform(delete("/api/boards/{boardId}", boardId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	// Scenario: deleting a board when user cannot delete returns 403
	// Given: Board exists but authorizationService.canDeleteBoard returns false
	// When: DELETE /api/boards/{boardId} is called
	// Then: the response is 403 Forbidden
	@Test
	void delete_userCannotDelete_returns403() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canDeleteBoard(user, board)).thenReturn(false);

		mockMvc.perform(delete("/api/boards/{boardId}", boardId))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	// Scenario: deleting a board when user can delete returns 204
	// Given: Board exists, user can delete, delete handler returns true
	// When: DELETE /api/boards/{boardId} is called
	// Then: the response is 204 No Content
	@Test
	void delete_success_returns204() throws Exception {
		UUID boardId = UUID.randomUUID();
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(authorizationService.canDeleteBoard(user, board)).thenReturn(true);
		when(deleteBoardCommandHandler.handle(boardId)).thenReturn(true);

		mockMvc.perform(delete("/api/boards/{boardId}", boardId))
				.andExpect(status().isNoContent());

		verify(deleteBoardCommandHandler).handle(boardId);
	}
}
