package com.pak.todo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.pak.todo.model.dto.LoginRequest;
import com.pak.todo.model.dto.LoginResponse;
import com.pak.todo.model.entity.User;
import com.pak.todo.security.AppUserDetails;
import com.pak.todo.security.JwtService;

class AuthControllerTest {

	// Scenario: login with valid credentials returns 200 and a token
	// Given: AuthenticationManager authenticates successfully and JwtService returns a token
	// When: login(request) is called with valid username and password
	// Then: response status is 200 and body contains the token
	@Test
	void login_validCredentials_returns200AndToken() {
		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		JwtService jwtService = mock(JwtService.class);
		User user = User.create(UUID.randomUUID(), "user", "hash");
		AppUserDetails userDetails = new AppUserDetails(user);
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
		when(jwtService.generateToken(eq(user))).thenReturn("jwt-token");

		AuthController controller = new AuthController(authenticationManager, jwtService);
		LoginRequest request = LoginRequest.builder().username("user").password("pass").build();
		ResponseEntity<LoginResponse> response = controller.login(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getToken()).isEqualTo("jwt-token");
		verify(jwtService).generateToken(user);
	}

	// Scenario: login with invalid credentials returns 401
	// Given: AuthenticationManager throws BadCredentialsException
	// When: login(request) is called with invalid username or password
	// Then: response status is 401 Unauthorized
	@Test
	void login_invalidCredentials_returns401() {
		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		JwtService jwtService = mock(JwtService.class);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		AuthController controller = new AuthController(authenticationManager, jwtService);
		LoginRequest request = LoginRequest.builder().username("user").password("wrong").build();
		ResponseEntity<LoginResponse> response = controller.login(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(jwtService, org.mockito.Mockito.never()).generateToken(any());
	}

	// Scenario: login when account is locked returns 401
	// Given: AuthenticationManager throws a non-BadCredentials AuthenticationException (e.g. LockedException)
	// When: login(request) is called
	// Then: response status is 401 Unauthorized and no token is generated
	@Test
	void login_otherAuthenticationException_returns401() {
		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		JwtService jwtService = mock(JwtService.class);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new LockedException("Account locked"));

		AuthController controller = new AuthController(authenticationManager, jwtService);
		LoginRequest request = LoginRequest.builder().username("user").password("pass").build();
		ResponseEntity<LoginResponse> response = controller.login(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(jwtService, org.mockito.Mockito.never()).generateToken(any());
	}
}
