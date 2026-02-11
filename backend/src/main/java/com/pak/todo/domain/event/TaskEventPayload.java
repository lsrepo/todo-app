package com.pak.todo.domain.event;

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
public class TaskEventPayload {

	private UUID id;
	private UUID boardId;
	private String name;
	private String description;
	private Instant dueDate;
	private TaskStatus status;
	private Instant createdAt;
	private Instant updatedAt;
	private String eventType;
	private Instant occurredAt;
}
