package com.pak.todo.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import com.pak.todo.model.dto.BoardCreateRequest;
import com.pak.todo.web.BoardController;

class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
	}

	// Scenario: validation failure returns 400 with VALIDATION_ERROR and field errors
	// Given: a MethodArgumentNotValidException with binding result containing field errors
	// When: handleValidation(ex) is called
	// Then: response is 400 Bad Request with code VALIDATION_ERROR, message "Validation failed", and errors list
	@Test
	void handleValidation_fieldErrors_returns400WithErrorBody() throws Exception {
		Method method = BoardController.class.getMethod("create", BoardCreateRequest.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(null, "request");
		bindingResult.addError(new FieldError("request", "name", "name is required"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

		ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
		assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
		assertThat(response.getBody().getErrors()).hasSize(1);
		assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("name");
		assertThat(response.getBody().getErrors().get(0).getMessage()).isEqualTo("name is required");
	}

	// Scenario: resource not found returns 404 with NOT_FOUND and message
	// Given: a ResourceNotFoundException with a message
	// When: handleNotFound(ex) is called
	// Then: response is 404 Not Found with code NOT_FOUND and the exception message
	@Test
	void handleNotFound_returns404WithMessage() {
		ResourceNotFoundException ex = new ResourceNotFoundException("Board not found: 123");

		ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo("NOT_FOUND");
		assertThat(response.getBody().getMessage()).isEqualTo("Board not found: 123");
		assertThat(response.getBody().getErrors()).isNull();
	}

	// Scenario: access denied with message returns 403 with FORBIDDEN and message
	// Given: an AccessDeniedException with a message
	// When: handleAccessDenied(ex) is called
	// Then: response is 403 Forbidden with code FORBIDDEN and the exception message
	@Test
	void handleAccessDenied_withMessage_returns403WithMessage() {
		AccessDeniedException ex = new AccessDeniedException("Access denied to board 456");

		ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo("FORBIDDEN");
		assertThat(response.getBody().getMessage()).isEqualTo("Access denied to board 456");
	}

	// Scenario: access denied with null message returns 403 with default message
	// Given: an AccessDeniedException with null message
	// When: handleAccessDenied(ex) is called
	// Then: response is 403 Forbidden with code FORBIDDEN and message "Access denied"
	@Test
	void handleAccessDenied_nullMessage_returns403WithDefaultMessage() {
		AccessDeniedException ex = new AccessDeniedException(null);

		ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo("FORBIDDEN");
		assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
	}
}
