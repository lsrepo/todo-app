package com.pak.todo.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.pak.todo.model.enums.TaskStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

	@Id
	@Column(updatable = false, nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "board_id", nullable = false, updatable = false)
	private Board board;

	@Column(nullable = false)
	private String name;

	private String description;

	private Instant dueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskStatus status;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public static Task create(UUID id, Board board, String name, String description, Instant dueDate, TaskStatus status) {
		Instant now = Instant.now();
		return Task.builder()
				.id(id)
				.board(board)
				.name(name)
				.description(description)
				.dueDate(dueDate)
				.status(status != null ? status : TaskStatus.NOT_STARTED)
				.createdAt(now)
				.updatedAt(now)
				.build();
	}
}
