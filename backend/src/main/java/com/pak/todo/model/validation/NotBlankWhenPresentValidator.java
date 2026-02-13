package com.pak.todo.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotBlankWhenPresentValidator implements ConstraintValidator<NotBlankWhenPresent, CharSequence> {

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		return value.toString().trim().length() > 0;
	}
}
