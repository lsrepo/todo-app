package com.pak.todo.web.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pak.todo.domain.command.CreateBoardCommand;
import com.pak.todo.domain.command.UpdateBoardCommand;
import com.pak.todo.model.dto.BoardCreateRequest;
import com.pak.todo.model.dto.BoardUpdateRequest;

class BoardCommandFactoryTest {

	private BoardCommandFactory factory;

	@BeforeEach
	void setUp() {
		factory = new BoardCommandFactory();
	}

	// Scenario: creating a board command from a valid request with name and description
	// Given: a BoardCreateRequest with name and description set
	// When: createBoard(request) is called
	// Then: a CreateBoardCommand is built with a generated boardId, the name, and the description
	@Test
	void createBoard_validRequest_buildsCreateBoardCommandWithDefaults() {
		BoardCreateRequest request = BoardCreateRequest.builder()
				.name("My Board")
				.description("Board description")
				.build();

		CreateBoardCommand command = factory.createBoard(request);

		assertThat(command).isNotNull();
		assertThat(command.getBoardId()).isNotNull();
		assertThat(command.getName()).isEqualTo("My Board");
		assertThat(command.getDescription()).isEqualTo("Board description");
	}

	// Scenario: creating a board command when description is null
	// Given: a BoardCreateRequest with name set and description null
	// When: createBoard(request) is called
	// Then: a CreateBoardCommand is built with description defaulted to empty string
	@Test
	void createBoard_nullDescription_defaultsToEmptyString() {
		BoardCreateRequest request = BoardCreateRequest.builder()
				.name("Board")
				.description(null)
				.build();

		CreateBoardCommand command = factory.createBoard(request);

		assertThat(command.getDescription()).isEqualTo("");
	}

	// Scenario: building an update board command from a valid request
	// Given: a boardId and BoardUpdateRequest with name and description
	// When: updateBoard(boardId, request) is called
	// Then: an UpdateBoardCommand is built with the boardId, name, and description
	@Test
	void updateBoard_validRequest_buildsUpdateBoardCommandWithDefaults() {
		UUID boardId = UUID.randomUUID();
		BoardUpdateRequest request = BoardUpdateRequest.builder()
				.name("Updated Board")
				.description("Updated description")
				.build();

		UpdateBoardCommand command = factory.updateBoard(boardId, request);

		assertThat(command).isNotNull();
		assertThat(command.getBoardId()).isEqualTo(boardId);
		assertThat(command.getName()).isEqualTo("Updated Board");
		assertThat(command.getDescription()).isEqualTo("Updated description");
	}

	// Scenario: updating a board when description is null
	// Given: a boardId and BoardUpdateRequest with name set and description null
	// When: updateBoard(boardId, request) is called
	// Then: an UpdateBoardCommand is built with description defaulted to empty string
	@Test
	void updateBoard_nullDescription_defaultsToEmptyString() {
		UUID boardId = UUID.randomUUID();
		BoardUpdateRequest request = BoardUpdateRequest.builder()
				.name("Updated")
				.description(null)
				.build();

		UpdateBoardCommand command = factory.updateBoard(boardId, request);

		assertThat(command.getDescription()).isEqualTo("");
	}
}
