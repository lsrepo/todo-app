package com.pak.todo.model.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponse {

	private UUID id;
	private String name;
	private String description;
	private Instant createdAt;
	private Instant updatedAt;
}
