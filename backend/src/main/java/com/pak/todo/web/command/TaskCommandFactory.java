package com.pak.todo.web.command;

import java.util.LinkedHashMap;
import java.util.Map;
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
		Map<String, Object> payload = new LinkedHashMap<>();
		if (request.getName() != null) {
			payload.put("name", request.getName());
		}
		if (request.getDescription() != null) {
			payload.put("description", request.getDescription());
		}
		if (request.getDueDate() != null) {
			payload.put("dueDate", request.getDueDate());
		}
		if (request.getStatus() != null) {
			payload.put("status", request.getStatus());
		}
		return UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name(request.getName())
				.description(request.getDescription())
				.dueDate(request.getDueDate())
				.status(request.getStatus())
				.payload(payload)
				.build();
	}
}
