package com.pak.todo.domain.command;

import com.pak.todo.model.dto.BoardUpdateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBoardCommand {

	private UUID boardId;
	private String name;
	private String description;

	public static UpdateBoardCommand from(UUID boardId, BoardUpdateRequest request) {
		return UpdateBoardCommand.builder()
				.boardId(boardId)
				.name(request.getName())
				.description(request.getDescription())
				.build();
	}
}
