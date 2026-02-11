package com.pak.todo.command;

import com.pak.todo.domain.command.UpdateBoardCommand;
import com.pak.todo.domain.event.BoardEventPayload;
import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.mapper.BoardMapper;
import com.pak.todo.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UpdateBoardCommandHandler {

	private final BoardRepository boardRepository;
	private final BoardMapper boardMapper;
	private final OutboxSupport outboxSupport;

	@Transactional
	public BoardResponse handle(UpdateBoardCommand command) {
		Board board = boardRepository.findById(command.getBoardId()).orElse(null);
		if (board == null) return null;

		board.setName(command.getName());
		board.setDescription(command.getDescription());
		board.setUpdatedAt(Instant.now());
		boardRepository.save(board);

		BoardEventPayload payload = BoardEventPayload.builder()
				.id(board.getId())
				.name(board.getName())
				.description(board.getDescription())
				.createdAt(board.getCreatedAt())
				.updatedAt(board.getUpdatedAt())
				.eventType("BoardUpdated")
				.occurredAt(Instant.now())
				.build();
		outboxSupport.saveOutbox("Board", board.getId().toString(), "BoardUpdated", payload);

		return boardMapper.toResponse(board);
	}
}
