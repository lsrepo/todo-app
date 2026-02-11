package com.pak.todo.model.dto;

import com.pak.todo.model.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskUpdateRequest {

	@NotBlank(message = "name is required")
	@Size(max = 255)
	private String name;

	@Size(max = 2000)
	private String description;

	private Instant dueDate;

	private TaskStatus status;
}
