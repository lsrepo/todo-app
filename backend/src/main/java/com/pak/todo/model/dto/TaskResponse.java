package com.pak.todo.model.dto;

import java.time.Instant;
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
public class TaskResponse {

	private UUID id;
	private UUID boardId;
	private String name;
	private String description;
	private Instant dueDate;
	private TaskStatus status;
	private Instant createdAt;
	private Instant updatedAt;
}
