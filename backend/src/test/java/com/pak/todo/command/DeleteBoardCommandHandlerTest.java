package com.pak.todo.command;

import com.pak.todo.domain.event.BoardEventPayload;
import com.pak.todo.model.entity.Board;
import com.pak.todo.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteBoardCommandHandlerTest {

	// Scenario: successfully deletes an existing board
	// Given: an existing board for the given boardId
	// When: handle() is called
	// Then: a BoardDeleted outbox event is recorded, the board is deleted, and true is returned
	@Test
	void handle_existingBoard_deletesBoardAndOutbox() {
		BoardRepository boardRepository = Mockito.mock(BoardRepository.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		DeleteBoardCommandHandler handler = new DeleteBoardCommandHandler(
				boardRepository,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		Board existing = Board.create(boardId, "name", "desc");

		when(boardRepository.findById(boardId)).thenReturn(Optional.of(existing));

		boolean result = handler.handle(boardId);

		assertThat(result).isTrue();

		ArgumentCaptor<BoardEventPayload> payloadCaptor = ArgumentCaptor.forClass(BoardEventPayload.class);
		verify(outboxSupport).saveOutbox(
				eq("Board"),
				eq(boardId.toString()),
				eq("BoardDeleted"),
				payloadCaptor.capture()
		);

		BoardEventPayload payload = payloadCaptor.getValue();
		assertThat(payload.getId()).isEqualTo(boardId);
		assertThat(payload.getName()).isEqualTo(existing.getName());
		assertThat(payload.getDescription()).isEqualTo(existing.getDescription());
		assertThat(payload.getCreatedAt()).isEqualTo(existing.getCreatedAt());
		assertThat(payload.getUpdatedAt()).isEqualTo(existing.getUpdatedAt());
		assertThat(payload.getEventType()).isEqualTo("BoardDeleted");
		assertThat(payload.getOccurredAt()).isNotNull();

		verify(boardRepository).delete(existing);
	}

	// Scenario: board does not exist
	// Given: no board can be found for the given boardId
	// When: handle() is called
	// Then: returns false and does NOT delete or publish an outbox event
	@Test
	void handle_missingBoard_returnsFalseAndDoesNothing() {
		BoardRepository boardRepository = Mockito.mock(BoardRepository.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		DeleteBoardCommandHandler handler = new DeleteBoardCommandHandler(
				boardRepository,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		when(boardRepository.findById(boardId)).thenReturn(Optional.empty());

		boolean result = handler.handle(boardId);

		assertThat(result).isFalse();
		verify(boardRepository, never()).delete(any());
		verify(outboxSupport, never()).saveOutbox(any(), any(), any(), any());
	}
}

