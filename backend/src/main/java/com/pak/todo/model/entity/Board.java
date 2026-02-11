package com.pak.todo.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

	@Id
	@Column(updatable = false, nullable = false)
	private UUID id;

	@Column(nullable = false)
	private String name;

	private String description;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public static Board create(UUID id, String name, String description) {
		Instant now = Instant.now();
		return Board.builder()
				.id(id)
				.name(name)
				.description(description)
				.createdAt(now)
				.updatedAt(now)
				.build();
	}
}
