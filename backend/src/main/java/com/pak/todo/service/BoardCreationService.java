package com.pak.todo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pak.todo.auth.AuthorizationService;
import com.pak.todo.command.CreateBoardCommandHandler;
import com.pak.todo.domain.command.CreateBoardCommand;
import com.pak.todo.model.dto.BoardCreateRequest;
import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.User;
import com.pak.todo.security.CurrentUserService;
import com.pak.todo.web.command.BoardCommandFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardCreationService {

	private final CreateBoardCommandHandler createBoardCommandHandler;
	private final BoardService boardService;
	private final AuthorizationService authorizationService;
	private final BoardCommandFactory boardCommandFactory;
	private final CurrentUserService currentUserService;

	@Transactional
	public BoardResponse createBoardWithOwner(BoardCreateRequest request) {
		User currentUser = currentUserService.getCurrentUserOrThrow();
		return createBoardWithOwner(currentUser, boardCommandFactory.createBoard(request));
	}

	/**
	 * Creates a board and grants the given user owner permission if missing. Used when the owner is known (e.g. seeding).
	 */
	@Transactional
	public BoardResponse createBoardWithOwner(User user, CreateBoardCommand command) {
		BoardResponse response = createBoardCommandHandler.handle(command);
		Board createdBoard = boardService.getEntityById(response.getId());
		if (createdBoard != null) {
			authorizationService.grantOwnerIfMissing(user, createdBoard);
		}
		return response;
	}
}
