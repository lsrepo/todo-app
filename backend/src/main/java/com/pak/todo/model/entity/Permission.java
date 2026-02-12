package com.pak.todo.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.pak.todo.model.enums.PermissionRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "permissions",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_permissions_user_board",
						columnNames = {"user_id", "board_id"}
				)
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

	@Id
	@Column(updatable = false, nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "board_id", nullable = false, updatable = false)
	private Board board;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PermissionRole role;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public static Permission create(UUID id, User user, Board board, PermissionRole role) {
		Instant now = Instant.now();
		return Permission.builder()
				.id(id)
				.user(user)
				.board(board)
				.role(role)
				.createdAt(now)
				.updatedAt(now)
				.build();
	}
}

