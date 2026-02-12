package com.pak.todo.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pak.todo.model.entity.User;
import com.pak.todo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional(readOnly = true)
	public long countUsers() {
		return userRepository.count();
	}

	@Transactional
	public User createUser(UUID id, String username, String rawPassword) {
		String passwordHash = passwordEncoder.encode(rawPassword);
		User user = User.create(id, username, passwordHash);
		return userRepository.save(user);
	}
}

