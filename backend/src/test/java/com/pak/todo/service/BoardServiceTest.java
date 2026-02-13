package com.pak.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.mapper.BoardMapper;
import com.pak.todo.repository.BoardRepository;

class BoardServiceTest {

	// Scenario: findAll returns a paginated list of board responses
	// Given: the repository returns a page of boards
	// When: findAll(pageable) is called
	// Then: the mapper is applied to each board and a page of BoardResponse is returned
	@Test
	void findAll_returnsPageOfBoardResponse() {
		BoardRepository boardRepository = mock(BoardRepository.class);
		BoardMapper boardMapper = mock(BoardMapper.class);
		Board board = Board.create(UUID.randomUUID(), "Board", "Desc");
		BoardResponse response = BoardResponse.builder()
				.id(board.getId())
				.name(board.getName())
				.description(board.getDescription())
				.createdAt(board.getCreatedAt())
				.updatedAt(board.getUpdatedAt())
				.build();
		PageImpl<Board> page = new PageImpl<>(List.of(board));
		when(boardRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(page);
		when(boardMapper.toResponse(board)).thenReturn(response);

		BoardService service = new BoardService(boardRepository, boardMapper);
		var result = service.findAll(org.springframework.data.domain.PageRequest.of(0, 20));

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0)).isSameAs(response);
		verify(boardMapper).toResponse(board);
	}

	// Scenario: findById returns the mapped response when board exists
	// Given: the repository has a board for the given id
	// When: findById(id) is called
	// Then: the mapper is applied and the BoardResponse is returned
	@Test
	void findById_existingBoard_returnsBoardResponse() {
		BoardRepository boardRepository = mock(BoardRepository.class);
		BoardMapper boardMapper = mock(BoardMapper.class);
		UUID id = UUID.randomUUID();
		Board board = Board.create(id, "Board", "Desc");
		BoardResponse response = BoardResponse.builder().id(id).name("Board").build();
		when(boardRepository.findById(id)).thenReturn(Optional.of(board));
		when(boardMapper.toResponse(board)).thenReturn(response);

		BoardService service = new BoardService(boardRepository, boardMapper);
		BoardResponse result = service.findById(id);

		assertThat(result).isSameAs(response);
		verify(boardMapper).toResponse(board);
	}

	// Scenario: findById returns null when board does not exist
	// Given: the repository has no board for the given id
	// When: findById(id) is called
	// Then: null is returned
	@Test
	void findById_missingBoard_returnsNull() {
		BoardRepository boardRepository = mock(BoardRepository.class);
		BoardMapper boardMapper = mock(BoardMapper.class);
		UUID id = UUID.randomUUID();
		when(boardRepository.findById(id)).thenReturn(Optional.empty());

		BoardService service = new BoardService(boardRepository, boardMapper);
		BoardResponse result = service.findById(id);

		assertThat(result).isNull();
		verify(boardMapper, org.mockito.Mockito.never()).toResponse(org.mockito.ArgumentMatchers.any());
	}

	// Scenario: getEntityById returns the entity when it exists
	// Given: the repository has a board for the given id
	// When: getEntityById(id) is called
	// Then: the Board entity is returned
	@Test
	void getEntityById_existingBoard_returnsBoard() {
		BoardRepository boardRepository = mock(BoardRepository.class);
		BoardMapper boardMapper = mock(BoardMapper.class);
		UUID id = UUID.randomUUID();
		Board board = Board.create(id, "Board", "Desc");
		when(boardRepository.findById(id)).thenReturn(Optional.of(board));

		BoardService service = new BoardService(boardRepository, boardMapper);
		Board result = service.getEntityById(id);

		assertThat(result).isSameAs(board);
	}

	// Scenario: getEntityById returns null when board does not exist
	// Given: the repository has no board for the given id
	// When: getEntityById(id) is called
	// Then: null is returned
	@Test
	void getEntityById_missingBoard_returnsNull() {
		BoardRepository boardRepository = mock(BoardRepository.class);
		BoardMapper boardMapper = mock(BoardMapper.class);
		UUID id = UUID.randomUUID();
		when(boardRepository.findById(id)).thenReturn(Optional.empty());

		BoardService service = new BoardService(boardRepository, boardMapper);
		Board result = service.getEntityById(id);

		assertThat(result).isNull();
	}
}
