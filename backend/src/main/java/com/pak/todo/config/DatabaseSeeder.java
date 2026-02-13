package com.pak.todo.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pak.todo.domain.command.CreateBoardCommand;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Permission;
import com.pak.todo.model.entity.User;
import com.pak.todo.model.enums.PermissionRole;
import com.pak.todo.repository.BoardRepository;
import com.pak.todo.repository.PermissionRepository;
import com.pak.todo.service.BoardCreationService;
import com.pak.todo.service.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

	private final UserService userService;
	private final BoardCreationService boardCreationService;
	private final BoardRepository boardRepository;
	private final PermissionRepository permissionRepository;

	@Override
	@Transactional
	public void run(String... args) {
		// Only seed when there are no users yet (e.g. after schema creation)
		if (userService.countUsers() > 0) {
			return;
		}

		String defaultPassword = "password";

		List<User> users = List.of(
				userService.createUser(UUID.randomUUID(), "user1", defaultPassword),
				userService.createUser(UUID.randomUUID(), "user2", defaultPassword),
				userService.createUser(UUID.randomUUID(), "user3", defaultPassword),
				userService.createUser(UUID.randomUUID(), "user4", defaultPassword),
				userService.createUser(UUID.randomUUID(), "user5", defaultPassword)
		);

		// For each user, create one board and give them OWNER permission
		List<UUID> boardIds = new ArrayList<>();
		for (User user : users) {
			CreateBoardCommand command = CreateBoardCommand.builder()
					.boardId(UUID.randomUUID())
					.name(user.getUsername() + "'s board")
					.description("Default board for " + user.getUsername())
					.build();
			var response = boardCreationService.createBoardWithOwner(user, command);
			boardIds.add(response.getId());
		}

		// user1 gets EDIT role on all boards except the one they own (first board)
		User user1 = users.get(0);
		UUID user1BoardId = boardIds.get(0);
		for (UUID boardId : boardIds) {
			if (boardId.equals(user1BoardId)) {
				continue;
			}
			Board board = boardRepository.findById(boardId).orElseThrow();
			Permission permission = Permission.create(UUID.randomUUID(), user1, board, PermissionRole.EDITOR);
			permissionRepository.save(permission);
		}
	}
}

