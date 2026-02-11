package com.pak.todo.command;

import com.pak.todo.domain.command.UpdateTaskCommand;
import com.pak.todo.domain.event.TaskEventPayload;
import com.pak.todo.model.dto.TaskResponse;
import com.pak.todo.model.entity.Task;
import com.pak.todo.model.mapper.TaskMapper;
import com.pak.todo.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UpdateTaskCommandHandler {

	private final TaskRepository taskRepository;
	private final TaskMapper taskMapper;
	private final OutboxSupport outboxSupport;

	@Transactional
	public TaskResponse handle(UpdateTaskCommand command) {
		Task task = taskRepository.findByIdAndBoardId(command.getTaskId(), command.getBoardId()).orElse(null);
		if (task == null) return null;

		task.setName(command.getName());
		task.setDescription(command.getDescription());
		task.setDueDate(command.getDueDate());
		if (command.getStatus() != null) {
			task.setStatus(command.getStatus());
		}
		task.setUpdatedAt(Instant.now());
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
				.eventType("TaskUpdated")
				.occurredAt(Instant.now())
				.build();
		outboxSupport.saveOutbox("Task", task.getId().toString(), "TaskUpdated", task.getBoard().getId(), payload);

		return taskMapper.toResponse(task);
	}
}
