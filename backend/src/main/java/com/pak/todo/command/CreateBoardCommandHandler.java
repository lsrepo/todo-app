package com.pak.todo.command;

import com.pak.todo.domain.command.CreateBoardCommand;
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
public class CreateBoardCommandHandler {

	private final BoardRepository boardRepository;
	private final BoardMapper boardMapper;
	private final OutboxSupport outboxSupport;

	@Transactional
	public BoardResponse handle(CreateBoardCommand command) {
		Board board = Board.create(command.getBoardId(), command.getName(), command.getDescription());
		boardRepository.save(board);

		BoardEventPayload payload = BoardEventPayload.builder()
				.id(board.getId())
				.name(board.getName())
				.description(board.getDescription())
				.createdAt(board.getCreatedAt())
				.updatedAt(board.getUpdatedAt())
				.eventType("BoardCreated")
				.occurredAt(Instant.now())
				.build();
		outboxSupport.saveOutbox("Board", board.getId().toString(), "BoardCreated", board.getId(), payload);

		return boardMapper.toResponse(board);
	}
}
