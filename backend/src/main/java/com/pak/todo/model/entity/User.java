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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@Column(updatable = false, nullable = false)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public static User create(UUID id, String username, String passwordHash) {
		Instant now = Instant.now();
		return User.builder()
				.id(id)
				.username(username)
				.passwordHash(passwordHash)
				.createdAt(now)
				.updatedAt(now)
				.build();
	}

	public void updatePasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
		this.updatedAt = Instant.now();
	}
}

