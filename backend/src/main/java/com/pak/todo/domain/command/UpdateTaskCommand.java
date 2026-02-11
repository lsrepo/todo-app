package com.pak.todo.domain.command;

import com.pak.todo.model.dto.TaskUpdateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskCommand {

	private UUID boardId;
	private UUID taskId;
	private String name;
	private String description;
	private Instant dueDate;
	private com.pak.todo.model.enums.TaskStatus status;

	public static UpdateTaskCommand from(UUID boardId, UUID taskId, TaskUpdateRequest request) {
		return UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name(request.getName())
				.description(request.getDescription())
				.dueDate(request.getDueDate())
				.status(request.getStatus())
				.build();
	}
}
