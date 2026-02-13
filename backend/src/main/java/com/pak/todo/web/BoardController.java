package com.pak.todo.web;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pak.todo.auth.AuthorizationService;
import com.pak.todo.command.CreateBoardCommandHandler;
import com.pak.todo.command.DeleteBoardCommandHandler;
import com.pak.todo.command.UpdateBoardCommandHandler;
import com.pak.todo.model.dto.BoardCreateRequest;
import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.dto.BoardUpdateRequest;
import com.pak.todo.web.command.BoardCommandFactory;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.User;
import com.pak.todo.security.CurrentUserService;
import com.pak.todo.service.BoardService;
import com.pak.todo.web.error.ResourceNotFoundException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Boards", description = "Board CRUD and listing")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

	private final BoardService boardService;
	private final BoardCommandFactory boardCommandFactory;
	private final CreateBoardCommandHandler createBoardCommandHandler;
	private final UpdateBoardCommandHandler updateBoardCommandHandler;
	private final DeleteBoardCommandHandler deleteBoardCommandHandler;
	private final AuthorizationService authorizationService;
	private final CurrentUserService currentUserService;

	@Operation(summary = "List all boards")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Paginated list of boards") })
	@GetMapping
	public Page<BoardResponse> list(@PageableDefault(size = 20) Pageable pageable) {
		return boardService.findAll(pageable);
	}

	@Operation(summary = "Get a board by ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Board found"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Board not found")
	})
	@GetMapping("/{boardId}")
	public BoardResponse get(@PathVariable UUID boardId) {
		Board board = boardService.getEntityById(boardId);
		if (board == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}

		User currentUser = currentUserService.getCurrentUserOrThrow();
		if (!authorizationService.canViewBoard(currentUser, board)) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied to board " + boardId);
		}

		return boardService.findById(boardId);
	}

	@Operation(summary = "Create a board")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Board created"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
	})
	@PostMapping
	public ResponseEntity<BoardResponse> create(@Valid @RequestBody BoardCreateRequest request) {
		User currentUser = currentUserService.getCurrentUserOrThrow();

		BoardResponse response = createBoardCommandHandler.handle(boardCommandFactory.createBoard(request));
		Board createdBoard = boardService.getEntityById(response.getId());
		if (createdBoard != null) {
			authorizationService.grantOwnerIfMissing(currentUser, createdBoard);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Update a board")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Board updated"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Board not found")
	})
	@PutMapping("/{boardId}")
	public BoardResponse update(@PathVariable UUID boardId, @Valid @RequestBody BoardUpdateRequest request) {
		Board board = boardService.getEntityById(boardId);
		if (board == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}

		User currentUser = currentUserService.getCurrentUserOrThrow();
		if (!authorizationService.canEditBoard(currentUser, board)) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied to board " + boardId);
		}

		BoardResponse response = updateBoardCommandHandler.handle(boardCommandFactory.updateBoard(boardId, request));
		if (response == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}
		return response;
	}

	@Operation(summary = "Delete a board")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Board deleted"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Board not found")
	})
	@DeleteMapping("/{boardId}")
	public ResponseEntity<Void> delete(@PathVariable UUID boardId) {
		Board board = boardService.getEntityById(boardId);
		if (board == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}

		User currentUser = currentUserService.getCurrentUserOrThrow();
		if (!authorizationService.canDeleteBoard(currentUser, board)) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied to board " + boardId);
		}

		boolean deleted = deleteBoardCommandHandler.handle(boardId);
		if (!deleted) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}
		return ResponseEntity.noContent().build();
	}
}
