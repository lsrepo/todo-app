package com.pak.todo.command;

import com.pak.todo.domain.command.UpdateTaskCommand;
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

		if (command.getName() != null) {
			task.setName(command.getName());
		}
		if (command.getDescription() != null) {
			task.setDescription(command.getDescription());
		}
		task.setDueDate(command.getDueDate());
		if (command.getStatus() != null) {
			task.setStatus(command.getStatus());
		}
		task.setUpdatedAt(Instant.now());
		taskRepository.save(task);

		outboxSupport.saveOutbox("Task", task.getId().toString(), "TaskUpdated", task.getBoard().getId(), command.getPayload());

		return taskMapper.toResponse(task);
	}
}
