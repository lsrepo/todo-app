package com.pak.todo.domain.command;

import com.pak.todo.model.dto.TaskCreateRequest;
import com.pak.todo.model.enums.TaskStatus;
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
public class CreateTaskCommand {

	private UUID taskId;
	private UUID boardId;
	private String name;
	private String description;
	private Instant dueDate;
	private TaskStatus status;

	public static CreateTaskCommand from(UUID boardId, TaskCreateRequest request) {
		return CreateTaskCommand.builder()
				.taskId(UUID.randomUUID())
				.boardId(boardId)
				.name(request.getName())
				.description(request.getDescription())
				.dueDate(request.getDueDate())
				.status(request.getStatus() != null ? request.getStatus() : TaskStatus.NOT_STARTED)
				.build();
	}
}
