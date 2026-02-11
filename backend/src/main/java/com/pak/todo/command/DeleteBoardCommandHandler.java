package com.pak.todo.command;

import com.pak.todo.domain.event.BoardEventPayload;
import com.pak.todo.model.entity.Board;
import com.pak.todo.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteBoardCommandHandler {

	private final BoardRepository boardRepository;
	private final OutboxSupport outboxSupport;

	@Transactional
	public boolean handle(UUID boardId) {
		Board board = boardRepository.findById(boardId).orElse(null);
		if (board == null) return false;

		BoardEventPayload payload = BoardEventPayload.builder()
				.id(board.getId())
				.name(board.getName())
				.description(board.getDescription())
				.createdAt(board.getCreatedAt())
				.updatedAt(board.getUpdatedAt())
				.eventType("BoardDeleted")
				.occurredAt(Instant.now())
				.build();
		outboxSupport.saveOutbox("Board", board.getId().toString(), "BoardDeleted", board.getId(), payload);

		boardRepository.delete(board);
		return true;
	}
}
