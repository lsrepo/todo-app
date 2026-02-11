package com.pak.todo.domain.command;

import com.pak.todo.model.dto.BoardCreateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBoardCommand {

	private UUID boardId;
	private String name;
	private String description;

	public static CreateBoardCommand from(BoardCreateRequest request) {
		return CreateBoardCommand.builder()
				.boardId(UUID.randomUUID())
				.name(request.getName())
				.description(request.getDescription())
				.build();
	}
}
