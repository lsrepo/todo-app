package com.pak.todo.model.mapper;

import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import org.springframework.stereotype.Component;

@Component
public class BoardMapper {

	public BoardResponse toResponse(Board board) {
		if (board == null) return null;
		return BoardResponse.builder()
				.id(board.getId())
				.name(board.getName())
				.description(board.getDescription())
				.createdAt(board.getCreatedAt())
				.updatedAt(board.getUpdatedAt())
				.build();
	}
}
