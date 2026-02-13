package com.pak.todo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pak.todo.model.dto.TaskCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerValidationTest extends AbstractTaskControllerTest {

	@Autowired
	private ObjectMapper objectMapper;

	// Scenario: creating a task with description longer than allowed
	// Given: a TaskCreateRequest with a valid name and a description over 2000 characters
	// When: POST /api/boards/{boardId}/tasks is called
	// Then: the request is rejected with 400 Bad Request and a VALIDATION_ERROR response containing a description field error
	@Test
	void create_invalidDescription_tooLong_returnsBadRequest() throws Exception {
		UUID boardId = UUID.randomUUID();

		String longDescription = "x".repeat(2001);
		TaskCreateRequest request = TaskCreateRequest.builder()
				.name("task-name")
				.description(longDescription)
				.dueDate(Instant.now())
				.build();

		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/boards/{boardId}/tasks", boardId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.errors[0].field").value("description"));

		verifyNoInteractions(createTaskCommandHandler);
	}

	// Scenario: creating a task with a blank name
	// Given: a TaskCreateRequest with a blank name and otherwise valid fields
	// When: POST /api/boards/{boardId}/tasks is called
	// Then: the request is rejected with 400 Bad Request and a VALIDATION_ERROR response containing a name field error
	@Test
	void create_invalidName_blank_returnsBadRequest() throws Exception {
		UUID boardId = UUID.randomUUID();

		TaskCreateRequest request = TaskCreateRequest.builder()
				.name("   ")
				.description("some description")
				.dueDate(Instant.now())
				.build();

		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/boards/{boardId}/tasks", boardId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").value("name is required"));

		verifyNoInteractions(createTaskCommandHandler);
	}
}
