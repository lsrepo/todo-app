package com.pak.todo.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

	private String code;
	private String message;
	private List<FieldError> errors;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class FieldError {
		private String field;
		private String message;
	}
}
