package com.pak.todo.web.command;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.pak.todo.domain.command.CreateTaskCommand;
import com.pak.todo.domain.command.UpdateTaskCommand;
import com.pak.todo.model.dto.TaskCreateRequest;
import com.pak.todo.model.dto.TaskUpdateRequest;
import com.pak.todo.model.enums.TaskStatus;

@Component
public class TaskCommandFactory {

	public CreateTaskCommand createTask(UUID boardId, TaskCreateRequest request) {
		return CreateTaskCommand.builder()
				.taskId(UUID.randomUUID())
				.boardId(boardId)
				.name(request.getName())
				.description(request.getDescription() != null ? request.getDescription() : "")
				.dueDate(request.getDueDate())
				.status(request.getStatus() != null ? request.getStatus() : TaskStatus.NOT_STARTED)
				.build();
	}

	public UpdateTaskCommand updateTask(UUID boardId, UUID taskId, TaskUpdateRequest request) {
		return UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name(request.getName())
				.description(request.getDescription() != null ? request.getDescription() : "")
				.dueDate(request.getDueDate())
				.status(request.getStatus())
				.build();
	}
}
