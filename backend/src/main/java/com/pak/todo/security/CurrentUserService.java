package com.pak.todo.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.pak.todo.model.entity.User;

@Service
public class CurrentUserService {

	public Optional<User> getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof AppUserDetails appUserDetails) {
			return Optional.of(appUserDetails.getUser());
		}

		return Optional.empty();
	}

	public User getCurrentUserOrThrow() {
		return getCurrentUser()
				.orElseThrow(() -> new IllegalStateException("No authenticated user in security context"));
	}
}

