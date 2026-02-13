package com.pak.todo.command;

import com.pak.todo.domain.command.CreateBoardCommand;
import com.pak.todo.domain.command.CreateTaskCommand;
import com.pak.todo.domain.command.UpdateTaskCommand;
import com.pak.todo.model.entity.Task;
import com.pak.todo.model.enums.TaskStatus;
import com.pak.todo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LastWriteWinsIT {

	@Autowired
	private CreateBoardCommandHandler createBoardCommandHandler;

	@Autowired
	private CreateTaskCommandHandler createTaskCommandHandler;

	@Autowired
	private UpdateTaskCommandHandler updateTaskCommandHandler;

	@Autowired
	private TaskRepository taskRepository;

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
			.withDatabaseName("todo")
			.withUsername("postgres")
			.withPassword("postgres");


	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	// Scenario: last-write-wins semantics for concurrent-style updates
	// Given: a board and task, then two sequential UpdateTaskCommand calls with different payloads
	// When: both updates are applied (first then second)
	// Then: the persisted task reflects only the second update (name, description, dueDate, status)
	@Test
	void twoUpdates_finalStateMatchesLastWrite() {
		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Instant dueDate = Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS);

		createBoardCommandHandler.handle(
				new CreateBoardCommand(boardId, "Board", "Board desc")
		);
		createTaskCommandHandler.handle(
				new CreateTaskCommand(taskId, boardId, "Initial", "Initial desc", dueDate, TaskStatus.NOT_STARTED)
		);

		Instant firstDue = dueDate.plus(1, java.time.temporal.ChronoUnit.DAYS);
		Map<String, Object> firstPayload = new LinkedHashMap<>();
		firstPayload.put("name", "First");
		firstPayload.put("description", "first desc");
		firstPayload.put("dueDate", firstDue);
		firstPayload.put("status", TaskStatus.IN_PROGRESS);
		UpdateTaskCommand first = UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name("First")
				.description("first desc")
				.dueDate(firstDue)
				.status(TaskStatus.IN_PROGRESS)
				.payload(firstPayload)
				.build();
		updateTaskCommandHandler.handle(first);

		Instant secondDue = dueDate.plus(2, java.time.temporal.ChronoUnit.DAYS);
		Map<String, Object> secondPayload = new LinkedHashMap<>();
		secondPayload.put("name", "Second");
		secondPayload.put("description", "second desc");
		secondPayload.put("dueDate", secondDue);
		secondPayload.put("status", TaskStatus.COMPLETED);
		UpdateTaskCommand second = UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name("Second")
				.description("second desc")
				.dueDate(secondDue)
				.status(TaskStatus.COMPLETED)
				.payload(secondPayload)
				.build();
		updateTaskCommandHandler.handle(second);

		Task task = taskRepository.findByIdAndBoardId(taskId, boardId).orElseThrow();
		assertThat(task.getName()).isEqualTo("Second");
		assertThat(task.getDescription()).isEqualTo("second desc");
		assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(task.getDueDate()).isEqualTo(dueDate.plus(2, java.time.temporal.ChronoUnit.DAYS));
	}
}
