package com.pak.todo.model.mapper;

import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

	public TaskResponse toResponse(Task task) {
		if (task == null) return null;
		return TaskResponse.builder()
				.id(task.getId())
				.boardId(task.getBoard().getId())
				.name(task.getName())
				.description(task.getDescription())
				.dueDate(task.getDueDate())
				.status(task.getStatus())
				.createdAt(task.getCreatedAt())
				.updatedAt(task.getUpdatedAt())
				.build();
	}
}
