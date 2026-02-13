package com.pak.todo.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

	@OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE)
	@Builder.Default
	private List<Permission> permissions = new ArrayList<>();

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
