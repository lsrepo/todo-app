package com.pak.todo.web.command;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.pak.todo.domain.command.CreateBoardCommand;
import com.pak.todo.domain.command.UpdateBoardCommand;
import com.pak.todo.model.dto.BoardCreateRequest;
import com.pak.todo.model.dto.BoardUpdateRequest;

@Component
public class BoardCommandFactory {

	public CreateBoardCommand createBoard(BoardCreateRequest request) {
		return CreateBoardCommand.builder()
				.boardId(UUID.randomUUID())
				.name(request.getName())
				.description(request.getDescription() != null ? request.getDescription() : "")
				.build();
	}

	public UpdateBoardCommand updateBoard(UUID boardId, BoardUpdateRequest request) {
		return UpdateBoardCommand.builder()
				.boardId(boardId)
				.name(request.getName())
				.description(request.getDescription() != null ? request.getDescription() : "")
				.build();
	}
}
