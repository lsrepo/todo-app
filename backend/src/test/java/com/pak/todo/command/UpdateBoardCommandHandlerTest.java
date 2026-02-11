package com.pak.todo.command;

import com.pak.todo.domain.command.UpdateBoardCommand;
import com.pak.todo.domain.event.BoardEventPayload;
import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.mapper.BoardMapper;
import com.pak.todo.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UpdateBoardCommandHandlerTest {

	// Scenario: successfully updates an existing board
	// Given: an UpdateBoardCommand for an existing board
	// When: handle() is called
	// Then: board fields are updated, saved, a BoardUpdated outbox event is recorded, and a BoardResponse is returned
	@Test
	void handle_existingBoard_updatesBoardAndOutbox() {
		BoardRepository boardRepository = Mockito.mock(BoardRepository.class);
		BoardMapper boardMapper = Mockito.mock(BoardMapper.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		UpdateBoardCommandHandler handler = new UpdateBoardCommandHandler(
				boardRepository,
				boardMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		Board board = Board.create(boardId, "old-name", "old-desc");
		Instant originalCreatedAt = board.getCreatedAt();
		Instant originalUpdatedAt = board.getUpdatedAt();

		when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));

		UpdateBoardCommand command = new UpdateBoardCommand(
				boardId,
				"new-name",
				"new-desc"
		);

		BoardResponse mappedResponse = new BoardResponse(
				boardId,
				command.getName(),
				command.getDescription(),
				originalCreatedAt,
				Instant.now()
		);
		when(boardMapper.toResponse(board)).thenReturn(mappedResponse);

		BoardResponse result = handler.handle(command);

		assertThat(result).isSameAs(mappedResponse);

		verify(boardRepository).save(board);
		assertThat(board.getName()).isEqualTo(command.getName());
		assertThat(board.getDescription()).isEqualTo(command.getDescription());
		assertThat(board.getCreatedAt()).isEqualTo(originalCreatedAt);
		assertThat(board.getUpdatedAt()).isNotNull();
		assertThat(board.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);

		ArgumentCaptor<BoardEventPayload> payloadCaptor = ArgumentCaptor.forClass(BoardEventPayload.class);
		verify(outboxSupport).saveOutbox(
				eq("Board"),
				eq(boardId.toString()),
				eq("BoardUpdated"),
				eq(boardId),
				payloadCaptor.capture()
		);

		BoardEventPayload payload = payloadCaptor.getValue();
		assertThat(payload.getId()).isEqualTo(boardId);
		assertThat(payload.getName()).isEqualTo(command.getName());
		assertThat(payload.getDescription()).isEqualTo(command.getDescription());
		assertThat(payload.getCreatedAt()).isEqualTo(originalCreatedAt);
		assertThat(payload.getUpdatedAt()).isEqualTo(board.getUpdatedAt());
		assertThat(payload.getEventType()).isEqualTo("BoardUpdated");
		assertThat(payload.getOccurredAt()).isNotNull();
	}

	// Scenario: board does not exist
	// Given: an UpdateBoardCommand for a board that cannot be found
	// When: handle() is called
	// Then: returns null and does NOT save or publish an outbox event
	@Test
	void handle_missingBoard_returnsNullAndDoesNothing() {
		BoardRepository boardRepository = Mockito.mock(BoardRepository.class);
		BoardMapper boardMapper = Mockito.mock(BoardMapper.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		UpdateBoardCommandHandler handler = new UpdateBoardCommandHandler(
				boardRepository,
				boardMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		when(boardRepository.findById(boardId)).thenReturn(Optional.empty());

		UpdateBoardCommand command = new UpdateBoardCommand(
				boardId,
				"new-name",
				"new-desc"
		);

		BoardResponse result = handler.handle(command);

		assertThat(result).isNull();
		verify(boardRepository, never()).save(any());
		verifyNoInteractions(boardMapper);
		verify(outboxSupport, never()).saveOutbox(any(), any(), any(), any(), any());
	}
}

