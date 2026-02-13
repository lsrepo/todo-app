package com.pak.todo.web.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pak.todo.domain.command.CreateTaskCommand;
import com.pak.todo.domain.command.UpdateTaskCommand;
import com.pak.todo.model.dto.TaskCreateRequest;
import com.pak.todo.model.dto.TaskUpdateRequest;
import com.pak.todo.model.enums.TaskStatus;

class TaskCommandFactoryTest {

	private TaskCommandFactory factory;

	@BeforeEach
	void setUp() {
		factory = new TaskCommandFactory();
	}

	// Scenario: creating a task command from a valid request with all fields set
	// Given: a boardId and TaskCreateRequest with name, description, dueDate, and status
	// When: createTask(boardId, request) is called
	// Then: a CreateTaskCommand is built with a generated taskId, boardId, and the request fields
	@Test
	void createTask_validRequest_buildsCreateTaskCommandWithDefaults() {
		UUID boardId = UUID.randomUUID();
		Instant dueDate = Instant.parse("2025-06-01T12:00:00Z");
		TaskCreateRequest request = TaskCreateRequest.builder()
				.name("Task name")
				.description("Task description")
				.dueDate(dueDate)
				.status(TaskStatus.IN_PROGRESS)
				.build();

		CreateTaskCommand command = factory.createTask(boardId, request);

		assertThat(command).isNotNull();
		assertThat(command.getTaskId()).isNotNull();
		assertThat(command.getBoardId()).isEqualTo(boardId);
		assertThat(command.getName()).isEqualTo("Task name");
		assertThat(command.getDescription()).isEqualTo("Task description");
		assertThat(command.getDueDate()).isEqualTo(dueDate);
		assertThat(command.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
	}

	// Scenario: creating a task command when description and status are null
	// Given: a boardId and TaskCreateRequest with description and status null
	// When: createTask(boardId, request) is called
	// Then: a CreateTaskCommand is built with description "" and status NOT_STARTED
	@Test
	void createTask_nullDescriptionAndStatus_defaultsToEmptyAndNotStarted() {
		UUID boardId = UUID.randomUUID();
		TaskCreateRequest request = TaskCreateRequest.builder()
				.name("Task")
				.description(null)
				.dueDate(Instant.now())
				.status(null)
				.build();

		CreateTaskCommand command = factory.createTask(boardId, request);

		assertThat(command.getDescription()).isEqualTo("");
		assertThat(command.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);
	}

	// Scenario: building an update task command from a valid request
	// Given: boardId, taskId, and TaskUpdateRequest with name, description, dueDate, status
	// When: updateTask(boardId, taskId, request) is called
	// Then: an UpdateTaskCommand is built with the given ids and request fields
	@Test
	void updateTask_validRequest_buildsUpdateTaskCommand() {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Instant dueDate = Instant.parse("2025-07-01T00:00:00Z");
		TaskUpdateRequest request = TaskUpdateRequest.builder()
				.name("Updated task")
				.description("Updated description")
				.dueDate(dueDate)
				.status(TaskStatus.COMPLETED)
				.build();

		UpdateTaskCommand command = factory.updateTask(boardId, taskId, request);

		assertThat(command).isNotNull();
		assertThat(command.getBoardId()).isEqualTo(boardId);
		assertThat(command.getTaskId()).isEqualTo(taskId);
		assertThat(command.getName()).isEqualTo("Updated task");
		assertThat(command.getDescription()).isEqualTo("Updated description");
		assertThat(command.getDueDate()).isEqualTo(dueDate);
		assertThat(command.getStatus()).isEqualTo(TaskStatus.COMPLETED);
	}

	// Scenario: updating a task when description is null
	// Given: boardId, taskId, and TaskUpdateRequest with description null
	// When: updateTask(boardId, taskId, request) is called
	// Then: an UpdateTaskCommand is built with description defaulted to empty string
	@Test
	void updateTask_nullDescription_defaultsToEmptyString() {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		TaskUpdateRequest request = TaskUpdateRequest.builder()
				.name("Updated")
				.description(null)
				.dueDate(Instant.now())
				.status(TaskStatus.IN_PROGRESS)
				.build();

		UpdateTaskCommand command = factory.updateTask(boardId, taskId, request);

		assertThat(command.getDescription()).isEqualTo("");
	}
}
