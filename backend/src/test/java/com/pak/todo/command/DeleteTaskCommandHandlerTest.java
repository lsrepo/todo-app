package com.pak.todo.command;

import com.pak.todo.domain.event.TaskEventPayload;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Task;
import com.pak.todo.model.enums.TaskStatus;
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
import static org.mockito.Mockito.when;

class DeleteTaskCommandHandlerTest {

	// Scenario: successfully deletes an existing task
	// Given: an existing task for the given boardId and taskId
	// When: handle() is called
	// Then: a TaskDeleted outbox event is recorded, the task is deleted, and true is returned
	@Test
	void handle_existingTask_deletesTaskAndOutbox() {
		TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		DeleteTaskCommandHandler handler = new DeleteTaskCommandHandler(
				taskRepository,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Board board = Board.create(boardId, "board", "desc");

		Task existing = Task.create(
				taskId,
				board,
				"name",
				"desc",
				Instant.now().plus(1, ChronoUnit.DAYS),
				TaskStatus.NOT_STARTED
		);

		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.of(existing));

		boolean result = handler.handle(boardId, taskId);

		assertThat(result).isTrue();

		ArgumentCaptor<TaskEventPayload> payloadCaptor = ArgumentCaptor.forClass(TaskEventPayload.class);
		verify(outboxSupport).saveOutbox(
				eq("Task"),
				eq(taskId.toString()),
				eq("TaskDeleted"),
				payloadCaptor.capture()
		);

		TaskEventPayload payload = payloadCaptor.getValue();
		assertThat(payload.getId()).isEqualTo(taskId);
		assertThat(payload.getBoardId()).isEqualTo(boardId);
		assertThat(payload.getName()).isEqualTo(existing.getName());
		assertThat(payload.getDescription()).isEqualTo(existing.getDescription());
		assertThat(payload.getDueDate()).isEqualTo(existing.getDueDate());
		assertThat(payload.getStatus()).isEqualTo(existing.getStatus());
		assertThat(payload.getCreatedAt()).isEqualTo(existing.getCreatedAt());
		assertThat(payload.getUpdatedAt()).isEqualTo(existing.getUpdatedAt());
		assertThat(payload.getEventType()).isEqualTo("TaskDeleted");
		assertThat(payload.getOccurredAt()).isNotNull();

		verify(taskRepository).delete(existing);
	}

	// Scenario: task does not exist
	// Given: no task can be found for the given boardId and taskId
	// When: handle() is called
	// Then: returns false and does NOT delete or publish an outbox event
	@Test
	void handle_missingTask_returnsFalseAndDoesNothing() {
		TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		DeleteTaskCommandHandler handler = new DeleteTaskCommandHandler(
				taskRepository,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();

		when(taskRepository.findByIdAndBoardId(taskId, boardId)).thenReturn(Optional.empty());

		boolean result = handler.handle(boardId, taskId);

		assertThat(result).isFalse();
		verify(taskRepository, never()).delete(Mockito.<Task>any());
		verify(outboxSupport, never()).saveOutbox(any(), any(), any(), any());
	}
}

