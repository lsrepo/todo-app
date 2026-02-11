package com.pak.todo.command;

import com.pak.todo.domain.command.UpdateTaskCommand;
import com.pak.todo.domain.event.TaskEventPayload;
import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Task;
import com.pak.todo.model.enums.TaskStatus;
import com.pak.todo.model.mapper.TaskMapper;
import com.pak.todo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

		UpdateTaskCommand command = new UpdateTaskCommand(
				boardId,
				taskId,
				"new-name",
				"new-desc",
				Instant.now().plus(2, ChronoUnit.DAYS),
				TaskStatus.IN_PROGRESS
		);

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

		ArgumentCaptor<TaskEventPayload> payloadCaptor = ArgumentCaptor.forClass(TaskEventPayload.class);
		verify(outboxSupport).saveOutbox(
				eq("Task"),
				eq(taskId.toString()),
				eq("TaskUpdated"),
				payloadCaptor.capture()
		);

		TaskEventPayload payload = payloadCaptor.getValue();
		assertThat(payload.getId()).isEqualTo(taskId);
		assertThat(payload.getBoardId()).isEqualTo(boardId);
		assertThat(payload.getName()).isEqualTo(command.getName());
		assertThat(payload.getDescription()).isEqualTo(command.getDescription());
		assertThat(payload.getDueDate()).isEqualTo(command.getDueDate());
		assertThat(payload.getStatus()).isEqualTo(command.getStatus());
		assertThat(payload.getCreatedAt()).isEqualTo(originalCreatedAt);
		assertThat(payload.getUpdatedAt()).isEqualTo(existing.getUpdatedAt());
		assertThat(payload.getEventType()).isEqualTo("TaskUpdated");
		assertThat(payload.getOccurredAt()).isNotNull();
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

		UpdateTaskCommand command = new UpdateTaskCommand(
				boardId,
				taskId,
				"new-name",
				"new-desc",
				Instant.now().plus(2, ChronoUnit.DAYS),
				null
		);

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

		UpdateTaskCommand command = new UpdateTaskCommand(
				boardId,
				taskId,
				"new-name",
				"new-desc",
				Instant.now().plus(2, ChronoUnit.DAYS),
				TaskStatus.NOT_STARTED
		);

		TaskResponse result = handler.handle(command);

		assertThat(result).isNull();
		verify(taskRepository, never()).save(any());
		verifyNoInteractions(taskMapper);
		verify(outboxSupport, never()).saveOutbox(any(), any(), any(), any());
	}
}

