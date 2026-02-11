package com.pak.todo.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pak.todo.command.CreateBoardCommandHandler;
import com.pak.todo.command.DeleteBoardCommandHandler;
import com.pak.todo.command.UpdateBoardCommandHandler;
import com.pak.todo.domain.command.CreateBoardCommand;
import com.pak.todo.domain.command.UpdateBoardCommand;
import com.pak.todo.model.dto.BoardCreateRequest;
import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.dto.BoardUpdateRequest;
import com.pak.todo.service.BoardService;
import com.pak.todo.web.error.ResourceNotFoundException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

	private final BoardService boardService;
	private final CreateBoardCommandHandler createBoardCommandHandler;
	private final UpdateBoardCommandHandler updateBoardCommandHandler;
	private final DeleteBoardCommandHandler deleteBoardCommandHandler;

	@GetMapping
	public Page<BoardResponse> list(@PageableDefault(size = 20) Pageable pageable) {
		return boardService.findAll(pageable);
	}

	@GetMapping("/{boardId}")
	public BoardResponse get(@PathVariable UUID boardId) {
		BoardResponse response = boardService.findById(boardId);
		if (response == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}
		return response;
	}

	@PostMapping
	public ResponseEntity<BoardResponse> create(@Valid @RequestBody BoardCreateRequest request) {
		CreateBoardCommand command = CreateBoardCommand.from(request);
		BoardResponse response = createBoardCommandHandler.handle(command);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{boardId}")
	public BoardResponse update(@PathVariable UUID boardId, @Valid @RequestBody BoardUpdateRequest request) {
		UpdateBoardCommand command = UpdateBoardCommand.from(boardId, request);
		BoardResponse response = updateBoardCommandHandler.handle(command);
		if (response == null) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}
		return response;
	}

	@DeleteMapping("/{boardId}")
	public ResponseEntity<Void> delete(@PathVariable UUID boardId) {
		boolean deleted = deleteBoardCommandHandler.handle(boardId);
		if (!deleted) {
			throw new ResourceNotFoundException("Board not found: " + boardId);
		}
		return ResponseEntity.noContent().build();
	}
}
