package com.pak.todo.command;

import com.pak.todo.domain.command.CreateTaskCommand;
import com.pak.todo.domain.event.TaskEventPayload;
import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Task;
import com.pak.todo.model.enums.TaskStatus;
import com.pak.todo.model.mapper.TaskMapper;
import com.pak.todo.repository.BoardRepository;
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

class CreateTaskCommandHandlerTest {

	// Scenario: successfully creates a task for an existing board
	// Given: a valid CreateTaskCommand with an existing boardId
	// When: handle() is called
	// Then: a Task is saved, an outbox TaskCreated event is recorded, and a TaskResponse is returned
	@Test
	void handle_existingBoard_createsTaskAndOutbox() {
		BoardRepository boardRepository = Mockito.mock(BoardRepository.class);
		TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
		TaskMapper taskMapper = new TaskMapper();
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		CreateTaskCommandHandler handler = new CreateTaskCommandHandler(
				boardRepository,
				taskRepository,
				taskMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Board board = Board.create(boardId, "board", "desc");
		when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));

		CreateTaskCommand command = new CreateTaskCommand(
				taskId,
				boardId,
				"task-name",
				"task-desc",
				Instant.now().plus(1, ChronoUnit.DAYS),
				TaskStatus.NOT_STARTED
		);

		TaskResponse result = handler.handle(command);
		assertThat(result).isNotNull();

		// verify task was saved with correct data
		ArgumentCaptor<Task> savedTaskCaptor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(savedTaskCaptor.capture());
		Task savedTask = savedTaskCaptor.getValue();
		assertThat(savedTask.getId()).isEqualTo(taskId);
		assertThat(savedTask.getBoard().getId()).isEqualTo(boardId);
		assertThat(savedTask.getName()).isEqualTo(command.getName());
		assertThat(savedTask.getDescription()).isEqualTo(command.getDescription());
		assertThat(savedTask.getDueDate()).isEqualTo(command.getDueDate());
		assertThat(savedTask.getStatus()).isEqualTo(command.getStatus());

		ArgumentCaptor<TaskEventPayload> payloadCaptor = ArgumentCaptor.forClass(TaskEventPayload.class);
		verify(outboxSupport).saveOutbox(
				eq("Task"),
				eq(taskId.toString()),
				eq("TaskCreated"),
				eq(boardId),
				payloadCaptor.capture()
		);

		TaskEventPayload payload = payloadCaptor.getValue();
		assertThat(payload.getId()).isEqualTo(taskId);
		assertThat(payload.getBoardId()).isEqualTo(boardId);
		assertThat(payload.getName()).isEqualTo(command.getName());
		assertThat(payload.getDescription()).isEqualTo(command.getDescription());
		assertThat(payload.getDueDate()).isEqualTo(command.getDueDate());
		assertThat(payload.getStatus()).isEqualTo(command.getStatus());
		assertThat(payload.getCreatedAt()).isNotNull();
		assertThat(payload.getUpdatedAt()).isNotNull();
		assertThat(payload.getEventType()).isEqualTo("TaskCreated");
		assertThat(payload.getOccurredAt()).isNotNull();

		// verify response mapping matches saved task
		TaskResponse expectedResponse = taskMapper.toResponse(savedTask);
		assertThat(result)
				.usingRecursiveComparison()
				.isEqualTo(expectedResponse);
	}

	// Scenario: board does not exist
	// Given: CreateTaskCommand with a non-existing boardId
	// When: handle() is called
	// Then: returns null and does NOT save a task or outbox event
	@Test
	void handle_missingBoard_returnsNullAndDoesNothing() {
		BoardRepository boardRepository = Mockito.mock(BoardRepository.class);
		TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
		TaskMapper taskMapper = Mockito.mock(TaskMapper.class);
		OutboxSupport outboxSupport = Mockito.mock(OutboxSupport.class);

		CreateTaskCommandHandler handler = new CreateTaskCommandHandler(
				boardRepository,
				taskRepository,
				taskMapper,
				outboxSupport
		);

		UUID boardId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		when(boardRepository.findById(boardId)).thenReturn(Optional.empty());

		CreateTaskCommand command = new CreateTaskCommand(
				taskId,
				boardId,
				"task-name",
				"task-desc",
				Instant.now().plus(1, ChronoUnit.DAYS),
				TaskStatus.NOT_STARTED
		);

		TaskResponse result = handler.handle(command);

		assertThat(result).isNull();
		verify(taskRepository, never()).save(any());
		verifyNoInteractions(taskMapper);
		verify(outboxSupport, never()).saveOutbox(any(), any(), any(),any(), any());
	}
}

