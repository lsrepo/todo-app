package com.pak.todo.command;

import com.pak.todo.domain.command.CreateBoardCommand;
import com.pak.todo.domain.event.BoardEventPayload;
import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.mapper.BoardMapper;
import com.pak.todo.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateBoardCommandHandlerTest {

	// Scenario: successfully creates a new board
	// Given: a valid CreateBoardCommand
	// When: handle() is called
	// Then: a Board is saved, a BoardCreated outbox event is recorded, and a BoardResponse is returned
	@Test
	void handle_createsBoardAndOutbox() {
		BoardRepository boardRepository = Mockito.mock(BoardRepository.class);
		BoardMapper boardMapper = Mockito.mock(BoardMapper.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		CreateBoardCommandHandler handler = new CreateBoardCommandHandler(
				boardRepository,
				boardMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		CreateBoardCommand command = new CreateBoardCommand(
				boardId,
				"name",
				"desc"
		);

		Board board = Board.create(boardId, command.getName(), command.getDescription());
		BoardResponse mappedResponse = new BoardResponse(
				board.getId(),
				board.getName(),
				board.getDescription(),
				board.getCreatedAt(),
				board.getUpdatedAt()
		);

		ArgumentCaptor<Board> savedBoardCaptor = ArgumentCaptor.forClass(Board.class);
		when(boardMapper.toResponse(any(Board.class))).thenReturn(mappedResponse);

		BoardResponse result = handler.handle(command);

		assertThat(result).isSameAs(mappedResponse);

		verify(boardRepository).save(savedBoardCaptor.capture());
		Board savedBoard = savedBoardCaptor.getValue();
		assertThat(savedBoard.getId()).isEqualTo(boardId);
		assertThat(savedBoard.getName()).isEqualTo(command.getName());
		assertThat(savedBoard.getDescription()).isEqualTo(command.getDescription());
		assertThat(savedBoard.getCreatedAt()).isNotNull();
		assertThat(savedBoard.getUpdatedAt()).isNotNull();

		ArgumentCaptor<BoardEventPayload> payloadCaptor = ArgumentCaptor.forClass(BoardEventPayload.class);
		verify(outboxSupport).saveOutbox(
				eq("Board"),
				eq(boardId.toString()),
				eq("BoardCreated"),
				payloadCaptor.capture()
		);

		BoardEventPayload payload = payloadCaptor.getValue();
		assertThat(payload.getId()).isEqualTo(boardId);
		assertThat(payload.getName()).isEqualTo(command.getName());
		assertThat(payload.getDescription()).isEqualTo(command.getDescription());
		assertThat(payload.getCreatedAt()).isEqualTo(savedBoard.getCreatedAt());
		assertThat(payload.getUpdatedAt()).isEqualTo(savedBoard.getUpdatedAt());
		assertThat(payload.getEventType()).isEqualTo("BoardCreated");
		assertThat(payload.getOccurredAt()).isNotNull();
	}
}

