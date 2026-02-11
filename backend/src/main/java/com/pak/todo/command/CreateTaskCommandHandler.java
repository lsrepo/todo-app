package com.pak.todo.command;

import com.pak.todo.domain.command.CreateTaskCommand;
import com.pak.todo.domain.event.TaskEventPayload;
import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Task;
import com.pak.todo.model.mapper.TaskMapper;
import com.pak.todo.repository.BoardRepository;
import com.pak.todo.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CreateTaskCommandHandler {

	private final BoardRepository boardRepository;
	private final TaskRepository taskRepository;
	private final TaskMapper taskMapper;
	private final OutboxSupport outboxSupport;

	@Transactional
	public TaskResponse handle(CreateTaskCommand command) {
		Board board = boardRepository.findById(command.getBoardId()).orElse(null);
		if (board == null) return null;

		Task task = Task.create(
				command.getTaskId(),
				board,
				command.getName(),
				command.getDescription(),
				command.getDueDate(),
				command.getStatus()
		);
		taskRepository.save(task);

		TaskEventPayload payload = TaskEventPayload.builder()
				.id(task.getId())
				.boardId(task.getBoard().getId())
				.name(task.getName())
				.description(task.getDescription())
				.dueDate(task.getDueDate())
				.status(task.getStatus())
				.createdAt(task.getCreatedAt())
				.updatedAt(task.getUpdatedAt())
				.eventType("TaskCreated")
				.occurredAt(Instant.now())
				.build();
		outboxSupport.saveOutbox("Task", task.getId().toString(), "TaskCreated", payload);

		return taskMapper.toResponse(task);
	}
}
