package com.pak.todo.config;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pak.todo.auth.AuthorizationService;
import com.pak.todo.command.CreateBoardCommandHandler;
import com.pak.todo.domain.command.CreateBoardCommand;
import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.User;
import com.pak.todo.service.BoardService;
import com.pak.todo.service.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

	private final UserService userService;
	private final BoardService boardService;
	private final CreateBoardCommandHandler createBoardCommandHandler;
	private final AuthorizationService authorizationService;

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
		for (User user : users) {
			CreateBoardCommand command = CreateBoardCommand.builder()
					.boardId(UUID.randomUUID())
					.name(user.getUsername() + "'s board")
					.description("Default board for " + user.getUsername())
					.build();

			BoardResponse response = createBoardCommandHandler.handle(command);
			Board board = boardService.getEntityById(response.getId());
			if (board != null) {
				authorizationService.grantOwnerIfMissing(user, board);
			}
		}
	}
}

