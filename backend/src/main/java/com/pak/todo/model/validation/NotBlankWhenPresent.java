package com.pak.todo.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotBlankWhenPresentValidator.class)
@Documented
public @interface NotBlankWhenPresent {

	String message() default "cannot be blank";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
