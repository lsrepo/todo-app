package com.pak.todo.web;

import com.pak.todo.model.dto.LoginRequest;
import com.pak.todo.model.dto.LoginResponse;
import com.pak.todo.security.AppUserDetails;
import com.pak.todo.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
			);

			AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
			String token = jwtService.generateToken(userDetails.getUser());
			return ResponseEntity.ok(new LoginResponse(token));
		}
		catch (BadCredentialsException ex) {
			return ResponseEntity.status(401).build();
		}
		catch (AuthenticationException ex) {
			return ResponseEntity.status(401).build();
		}
	}
}

