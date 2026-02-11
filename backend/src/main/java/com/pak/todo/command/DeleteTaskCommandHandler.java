package com.pak.todo.command;

import com.pak.todo.domain.event.TaskEventPayload;
import com.pak.todo.model.entity.Task;
import com.pak.todo.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteTaskCommandHandler {

	private final TaskRepository taskRepository;
	private final OutboxSupport outboxSupport;

	@Transactional
	public boolean handle(UUID boardId, UUID taskId) {
		Task task = taskRepository.findByIdAndBoardId(taskId, boardId).orElse(null);
		if (task == null) return false;

		TaskEventPayload payload = TaskEventPayload.builder()
				.id(task.getId())
				.boardId(task.getBoard().getId())
				.name(task.getName())
				.description(task.getDescription())
				.dueDate(task.getDueDate())
				.status(task.getStatus())
				.createdAt(task.getCreatedAt())
				.updatedAt(task.getUpdatedAt())
				.eventType("TaskDeleted")
				.occurredAt(Instant.now())
				.build();
		outboxSupport.saveOutbox("Task", task.getId().toString(), "TaskDeleted", payload);

		taskRepository.delete(task);
		return true;
	}
}
