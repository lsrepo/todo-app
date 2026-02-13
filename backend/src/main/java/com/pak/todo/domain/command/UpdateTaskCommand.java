package com.pak.todo.domain.command;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.pak.todo.model.enums.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
	private TaskStatus status;
	private Map<String, Object> payload;
}
