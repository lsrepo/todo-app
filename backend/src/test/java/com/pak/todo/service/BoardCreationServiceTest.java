package com.pak.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pak.todo.auth.AuthorizationService;
import com.pak.todo.command.CreateBoardCommandHandler;
import com.pak.todo.domain.command.CreateBoardCommand;
import com.pak.todo.model.dto.BoardCreateRequest;
import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.User;
import com.pak.todo.security.CurrentUserService;
import com.pak.todo.web.command.BoardCommandFactory;

@ExtendWith(MockitoExtension.class)
class BoardCreationServiceTest {

	@Mock
	private CreateBoardCommandHandler createBoardCommandHandler;

	@Mock
	private BoardService boardService;

	@Mock
	private AuthorizationService authorizationService;

	@Mock
	private BoardCommandFactory boardCommandFactory;

	@Mock
	private CurrentUserService currentUserService;

	@InjectMocks
	private BoardCreationService boardCreationService;

	// Scenario: creating a board with valid request delegates to handler and grants owner
	// Given: current user, factory returns a CreateBoardCommand, handler returns BoardResponse, board is found
	// When: createBoardWithOwner(request) is called
	// Then: createBoard is invoked on factory, handle is called with that command, grantOwnerIfMissing is called with current user and created board
	@Test
	void createBoardWithOwner_validRequest_callsHandlerAndGrantsOwner() {
		User user = User.create(UUID.randomUUID(), "user", "hash");
		BoardCreateRequest request = BoardCreateRequest.builder()
				.name("New Board")
				.description("Desc")
				.build();
		UUID boardId = UUID.randomUUID();
		CreateBoardCommand command = CreateBoardCommand.builder()
				.boardId(boardId)
				.name("New Board")
				.description("Desc")
				.build();
		BoardResponse response = BoardResponse.builder()
				.id(boardId)
				.name("New Board")
				.description("Desc")
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		Board createdBoard = Board.create(boardId, "New Board", "Desc");

		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(boardCommandFactory.createBoard(request)).thenReturn(command);
		when(createBoardCommandHandler.handle(command)).thenReturn(response);
		when(boardService.getEntityById(boardId)).thenReturn(createdBoard);

		BoardResponse result = boardCreationService.createBoardWithOwner(request);

		assertThat(result).isSameAs(response);
		verify(boardCommandFactory).createBoard(request);
		verify(createBoardCommandHandler).handle(same(command));
		verify(boardService).getEntityById(boardId);
		verify(authorizationService).grantOwnerIfMissing(same(user), same(createdBoard));
	}

	// Scenario: when created board is not found by id, owner is not granted
	// Given: handler returns BoardResponse but getEntityById returns null
	// When: createBoardWithOwner(request) is called
	// Then: handle is called, grantOwnerIfMissing is not called
	@Test
	void createBoardWithOwner_boardNotFound_doesNotGrantOwner() {
		User user = User.create(UUID.randomUUID(), "user", "hash");
		BoardCreateRequest request = BoardCreateRequest.builder().name("Board").description("").build();
		CreateBoardCommand command = CreateBoardCommand.builder().boardId(UUID.randomUUID()).name("Board").description("").build();
		BoardResponse response = BoardResponse.builder()
				.id(command.getBoardId())
				.name("Board")
				.description("")
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();

		when(currentUserService.getCurrentUserOrThrow()).thenReturn(user);
		when(boardCommandFactory.createBoard(any())).thenReturn(command);
		when(createBoardCommandHandler.handle(command)).thenReturn(response);
		when(boardService.getEntityById(command.getBoardId())).thenReturn(null);

		BoardResponse result = boardCreationService.createBoardWithOwner(request);

		assertThat(result).isSameAs(response);
		verify(createBoardCommandHandler).handle(command);
		verify(boardService).getEntityById(command.getBoardId());
		verify(authorizationService, never()).grantOwnerIfMissing(any(), any());
	}
}
