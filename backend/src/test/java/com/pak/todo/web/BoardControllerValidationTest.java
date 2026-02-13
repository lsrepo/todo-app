package com.pak.todo.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BoardControllerValidationTest extends AbstractBoardControllerTest {

	// Scenario: creating a board with a blank name
	// Given: a BoardCreateRequest with a blank name and otherwise valid fields
	// When: POST /api/boards is called
	// Then: the request is rejected with 400 Bad Request and a VALIDATION_ERROR response containing a name field error
	@Test
	void create_invalidName_blank_returnsBadRequest() throws Exception {
		String json = """
				{"name":"   ","description":"some description"}
				""";

		mockMvc.perform(post("/api/boards")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").value("name is required"));

		verifyNoInteractions(boardCreationService);
	}

	// Scenario: creating a board with description longer than allowed
	// Given: a BoardCreateRequest with a valid name and description over 1000 characters
	// When: POST /api/boards is called
	// Then: the request is rejected with 400 Bad Request and a VALIDATION_ERROR response containing a description field error
	@Test
	void create_invalidDescription_tooLong_returnsBadRequest() throws Exception {
		String longDescription = "x".repeat(1001);
		String json = String.format("{\"name\":\"Board\",\"description\":\"%s\"}", longDescription);

		mockMvc.perform(post("/api/boards")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.errors[0].field").value("description"));

		verifyNoInteractions(boardCreationService);
	}
}
