package com.pak.todo.domain.event;

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
public class BoardEventPayload {

	private UUID id;
	private String name;
	private String description;
	private Instant createdAt;
	private Instant updatedAt;
	private String eventType;
	private Instant occurredAt;
}
