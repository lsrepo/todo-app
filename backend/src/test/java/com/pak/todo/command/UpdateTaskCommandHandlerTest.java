package com.pak.todo.command;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pak.todo.domain.command.UpdateTaskCommand;
import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Task;
import com.pak.todo.model.enums.TaskStatus;
import com.pak.todo.model.mapper.TaskMapper;
import com.pak.todo.repository.TaskRepository;

class UpdateTaskCommandHandlerTest {

	// Scenario: successfully updates an existing task
	// Given: an UpdateTaskCommand for an existing task and board
	// When: handle() is called
	// Then: task fields are updated, saved, an outbox TaskUpdated event is recorded, and a TaskResponse is returned
	@Test
	void handle_existingTask_updatesTaskAndOutbox() {
		TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
		TaskMapper taskMapper = Mockito.mock(TaskMapper.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		UpdateTaskCommandHandler handler = new UpdateTaskCommandHandler(
				taskRepository,
				taskMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Board board = Board.create(boardId, "board", "desc");

		Task existing = Task.create(
				taskId,
				board,
				"old-name",
				"old-desc",
				Instant.now().plus(1, ChronoUnit.DAYS),
				TaskStatus.NOT_STARTED
		);
		Instant originalCreatedAt = existing.getCreatedAt();
		Instant originalUpdatedAt = existing.getUpdatedAt();

		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.of(existing));

		Instant newDueDate = Instant.now().plus(2, ChronoUnit.DAYS);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", "new-name");
		payload.put("description", "new-desc");
		payload.put("dueDate", newDueDate);
		payload.put("status", TaskStatus.IN_PROGRESS);
		UpdateTaskCommand command = UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name("new-name")
				.description("new-desc")
				.dueDate(newDueDate)
				.status(TaskStatus.IN_PROGRESS)
				.payload(payload)
				.build();

		TaskResponse mappedResponse = new TaskResponse(
				taskId,
				boardId,
				command.getName(),
				command.getDescription(),
				command.getDueDate(),
				command.getStatus(),
				originalCreatedAt,
				Instant.now()
		);
		when(taskMapper.toResponse(existing)).thenReturn(mappedResponse);

		TaskResponse result = handler.handle(command);

		assertThat(result).isSameAs(mappedResponse);

		verify(taskRepository).save(existing);
		assertThat(existing.getName()).isEqualTo(command.getName());
		assertThat(existing.getDescription()).isEqualTo(command.getDescription());
		assertThat(existing.getDueDate()).isEqualTo(command.getDueDate());
		assertThat(existing.getStatus()).isEqualTo(command.getStatus());
		assertThat(existing.getCreatedAt()).isEqualTo(originalCreatedAt);
		assertThat(existing.getUpdatedAt()).isNotNull();
		assertThat(existing.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(outboxSupport).saveOutbox(
				eq("Task"),
				eq(taskId.toString()),
				eq("TaskUpdated"),
				eq(boardId),
				payloadCaptor.capture()
		);

		Map<String, Object> savedPayload = payloadCaptor.getValue();
		assertThat(savedPayload).containsEntry("name", "new-name");
		assertThat(savedPayload).containsEntry("description", "new-desc");
		assertThat(savedPayload).containsEntry("dueDate", newDueDate);
		assertThat(savedPayload).containsEntry("status", TaskStatus.IN_PROGRESS);
		assertThat(savedPayload).hasSize(4);
	}

	// Scenario: null status should not override existing status
	// Given: an UpdateTaskCommand with status = null for an existing task
	// When: handle() is called
	// Then: status remains unchanged while other fields are updated
	@Test
	void handle_nullStatus_doesNotOverrideExistingStatus() {
		TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
		TaskMapper taskMapper = Mockito.mock(TaskMapper.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		UpdateTaskCommandHandler handler = new UpdateTaskCommandHandler(
				taskRepository,
				taskMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Board board = Board.create(boardId, "board", "desc");

		Task existing = Task.create(
				taskId,
				board,
				"old-name",
				"old-desc",
				Instant.now().plus(1, ChronoUnit.DAYS),
				TaskStatus.IN_PROGRESS
		);
		TaskStatus originalStatus = existing.getStatus();

		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.of(existing));

		Instant newDueDate = Instant.now().plus(2, ChronoUnit.DAYS);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", "new-name");
		payload.put("description", "new-desc");
		payload.put("dueDate", newDueDate);
		UpdateTaskCommand command = UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name("new-name")
				.description("new-desc")
				.dueDate(newDueDate)
				.status(null)
				.payload(payload)
				.build();

		when(taskMapper.toResponse(existing)).thenReturn(Mockito.mock(TaskResponse.class));

		handler.handle(command);

		verify(taskRepository).save(existing);
		assertThat(existing.getName()).isEqualTo(command.getName());
		assertThat(existing.getDescription()).isEqualTo(command.getDescription());
		assertThat(existing.getDueDate()).isEqualTo(command.getDueDate());
		assertThat(existing.getStatus()).isEqualTo(originalStatus);
	}

	// Scenario: task does not exist
	// Given: an UpdateTaskCommand for a task that cannot be found for the given board
	// When: handle() is called
	// Then: returns null and does NOT save the task or publish an outbox event
	@Test
	void handle_missingTask_returnsNullAndDoesNothing() {
		TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
		TaskMapper taskMapper = Mockito.mock(TaskMapper.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		UpdateTaskCommandHandler handler = new UpdateTaskCommandHandler(
				taskRepository,
				taskMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();

		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.empty());

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", "new-name");
		payload.put("description", "new-desc");
		UpdateTaskCommand command = UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name("new-name")
				.description("new-desc")
				.dueDate(Instant.now().plus(2, ChronoUnit.DAYS))
				.status(TaskStatus.NOT_STARTED)
				.payload(payload)
				.build();

		TaskResponse result = handler.handle(command);

		assertThat(result).isNull();
		verify(taskRepository, never()).save(any());
		verifyNoInteractions(taskMapper);
		verify(outboxSupport, never()).saveOutbox(any(), any(), any(), any(), any());
	}

	// Scenario: when name is null in the command, existing task name is not overwritten
	// Given: an UpdateTaskCommand with name = null for an existing task
	// When: handle() is called
	// Then: task name remains unchanged, other fields (description, dueDate, status) are updated
	@Test
	void handle_nullName_doesNotOverwriteExistingName() {
		TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
		TaskMapper taskMapper = Mockito.mock(TaskMapper.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		UpdateTaskCommandHandler handler = new UpdateTaskCommandHandler(
				taskRepository,
				taskMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Board board = Board.create(boardId, "board", "desc");

		Task existing = Task.create(
				taskId,
				board,
				"original-name",
				"old-desc",
				Instant.now().plus(1, ChronoUnit.DAYS),
				TaskStatus.NOT_STARTED
		);
		String originalName = existing.getName();

		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.of(existing));

		Instant newDueDate = Instant.now().plus(2, ChronoUnit.DAYS);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("description", "new-desc");
		payload.put("dueDate", newDueDate);
		payload.put("status", TaskStatus.IN_PROGRESS);
		UpdateTaskCommand command = UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name(null)
				.description("new-desc")
				.dueDate(newDueDate)
				.status(TaskStatus.IN_PROGRESS)
				.payload(payload)
				.build();

		when(taskMapper.toResponse(existing)).thenReturn(Mockito.mock(TaskResponse.class));

		handler.handle(command);

		verify(taskRepository).save(existing);
		assertThat(existing.getName()).isEqualTo(originalName);
		assertThat(existing.getDescription()).isEqualTo("new-desc");
		assertThat(existing.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
	}

	// Scenario: when only dueDate is updated, outbox payload contains only dueDate
	// Given: an UpdateTaskCommand with only dueDate set in the payload
	// When: handle() is called
	// Then: the outbox is saved with payload containing only dueDate
	@Test
	void handle_onlyDueDateInPayload_savesDueDateOnlyToOutbox() {
		TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
		TaskMapper taskMapper = Mockito.mock(TaskMapper.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		UpdateTaskCommandHandler handler = new UpdateTaskCommandHandler(
				taskRepository,
				taskMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Board board = Board.create(boardId, "board", "desc");
		Task existing = Task.create(
				taskId,
				board,
				"task-name",
				"desc",
				Instant.now().plus(1, ChronoUnit.DAYS),
				TaskStatus.NOT_STARTED
		);

		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.of(existing));

		Instant newDueDate = Instant.parse("2022-11-11T00:00:00Z");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("dueDate", newDueDate);
		UpdateTaskCommand command = UpdateTaskCommand.builder()
				.boardId(boardId)
				.taskId(taskId)
				.name(null)
				.description(null)
				.dueDate(newDueDate)
				.status(null)
				.payload(payload)
				.build();

		when(taskMapper.toResponse(existing)).thenReturn(Mockito.mock(TaskResponse.class));

		handler.handle(command);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(outboxSupport).saveOutbox(
				eq("Task"),
				eq(taskId.toString()),
				eq("TaskUpdated"),
				eq(boardId),
				payloadCaptor.capture()
		);
		Map<String, Object> savedPayload = payloadCaptor.getValue();
		assertThat(savedPayload).containsOnlyKeys("dueDate");
		assertThat(savedPayload.get("dueDate")).isEqualTo(newDueDate);
	}
}

