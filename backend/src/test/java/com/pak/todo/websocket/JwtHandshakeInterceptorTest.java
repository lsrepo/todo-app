package com.pak.todo.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import com.pak.todo.model.entity.User;
import com.pak.todo.repository.UserRepository;
import com.pak.todo.security.JwtPrincipal;
import com.pak.todo.security.JwtService;

class JwtHandshakeInterceptorTest {

	private JwtService jwtService;
	private UserRepository userRepository;
	private JwtHandshakeInterceptor interceptor;

	@BeforeEach
	void setUp() {
		jwtService = mock(JwtService.class);
		userRepository = mock(UserRepository.class);
		interceptor = new JwtHandshakeInterceptor(jwtService, userRepository);
	}

	private ServerHttpRequest requestWithHeaders(HttpHeaders headers) {
		ServerHttpRequest request = mock(ServerHttpRequest.class);
		when(request.getHeaders()).thenReturn(headers);
		return request;
	}

	// Scenario: handshake with no Authorization header and no Sec-WebSocket-Protocol returns false
	// Given: request has no Authorization header and no Sec-WebSocket-Protocol (or empty)
	// When: beforeHandshake is called
	// Then: returns false and attributes are not modified
	@Test
	void beforeHandshake_noAuthHeaderAndNoProtocol_returnsFalse() {
		HttpHeaders headers = new HttpHeaders();
		ServerHttpRequest request = requestWithHeaders(headers);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		WebSocketHandler wsHandler = mock(WebSocketHandler.class);
		Map<String, Object> attributes = new HashMap<>();

		boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

		assertThat(result).isFalse();
		assertThat(attributes).isEmpty();
	}

	// Scenario: handshake with valid Bearer token and user found returns true and puts user in attributes
	// Given: Authorization: Bearer <token>, JwtService returns principal, UserRepository returns user
	// When: beforeHandshake is called
	// Then: returns true and attributes contain "user"
	@Test
	void beforeHandshake_bearerTokenValidAndUserFound_returnsTrueAndPutsUserInAttributes() {
		String token = "valid-jwt";
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + token);
		ServerHttpRequest request = requestWithHeaders(headers);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		WebSocketHandler wsHandler = mock(WebSocketHandler.class);
		Map<String, Object> attributes = new HashMap<>();

		UUID userId = UUID.randomUUID();
		JwtPrincipal principal = new JwtPrincipal(userId, "user");
		User user = User.create(userId, "user", "hash");
		when(jwtService.parseAndValidate(token)).thenReturn(Optional.of(principal));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

		assertThat(result).isTrue();
		assertThat(attributes).containsKey("user");
		assertThat(attributes.get("user")).isSameAs(user);
	}

	// Scenario: handshake with Bearer token that fails validation returns false
	// Given: Authorization: Bearer <token>, JwtService.parseAndValidate returns empty
	// When: beforeHandshake is called
	// Then: returns false
	@Test
	void beforeHandshake_bearerTokenInvalid_returnsFalse() {
		String token = "invalid-jwt";
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + token);
		ServerHttpRequest request = requestWithHeaders(headers);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		WebSocketHandler wsHandler = mock(WebSocketHandler.class);
		Map<String, Object> attributes = new HashMap<>();

		when(jwtService.parseAndValidate(token)).thenReturn(Optional.empty());

		boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

		assertThat(result).isFalse();
		assertThat(attributes).isEmpty();
	}

	// Scenario: handshake with token in Sec-WebSocket-Protocol second entry, valid token and user returns true
	// Given: Sec-WebSocket-Protocol: board-v1, <jwt>, JwtService and UserRepository return valid principal and user
	// When: beforeHandshake is called
	// Then: returns true and attributes contain user
	@Test
	void beforeHandshake_tokenInSecWebSocketProtocolSecondEntry_validAndUserFound_returnsTrue() {
		String token = "protocol-jwt";
		HttpHeaders headers = new HttpHeaders();
		headers.add("Sec-WebSocket-Protocol", "board-v1, " + token);
		ServerHttpRequest request = requestWithHeaders(headers);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		WebSocketHandler wsHandler = mock(WebSocketHandler.class);
		Map<String, Object> attributes = new HashMap<>();

		UUID userId = UUID.randomUUID();
		JwtPrincipal principal = new JwtPrincipal(userId, "u");
		User user = User.create(userId, "u", "h");
		when(jwtService.parseAndValidate(token)).thenReturn(Optional.of(principal));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

		assertThat(result).isTrue();
		assertThat(attributes.get("user")).isSameAs(user);
	}

	// Scenario: handshake with Sec-WebSocket-Protocol containing only one value (no JWT) returns false
	// Given: Sec-WebSocket-Protocol: board-v1 only (no second entry for token)
	// When: beforeHandshake is called
	// Then: returns false (token remains null, parseAndValidate not called with token from protocol)
	@Test
	void beforeHandshake_tokenInSecWebSocketProtocol_singleProtocol_returnsFalse() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Sec-WebSocket-Protocol", "board-v1");
		ServerHttpRequest request = requestWithHeaders(headers);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		WebSocketHandler wsHandler = mock(WebSocketHandler.class);
		Map<String, Object> attributes = new HashMap<>();

		boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

		assertThat(result).isFalse();
		verify(jwtService, never()).parseAndValidate(any());
	}

	// Scenario: handshake with valid token but user not found in repository returns false
	// Given: Bearer token valid, UserRepository.findById returns empty
	// When: beforeHandshake is called
	// Then: returns false
	@Test
	void beforeHandshake_validToken_userNotFound_returnsFalse() {
		String token = "valid-jwt";
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + token);
		ServerHttpRequest request = requestWithHeaders(headers);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		WebSocketHandler wsHandler = mock(WebSocketHandler.class);
		Map<String, Object> attributes = new HashMap<>();

		UUID userId = UUID.randomUUID();
		JwtPrincipal principal = new JwtPrincipal(userId, "user");
		when(jwtService.parseAndValidate(token)).thenReturn(Optional.of(principal));
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

		assertThat(result).isFalse();
		assertThat(attributes).isEmpty();
	}

	// Scenario: afterHandshake is a no-op and does not throw
	// Given: any request, response, handler, exception
	// When: afterHandshake is called
	// Then: no exception is thrown
	@Test
	void afterHandshake_noOp_doesNotThrow() {
		ServerHttpRequest request = requestWithHeaders(new HttpHeaders());
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		WebSocketHandler wsHandler = mock(WebSocketHandler.class);

		interceptor.afterHandshake(request, response, wsHandler, null);
		interceptor.afterHandshake(request, response, wsHandler, new RuntimeException("test"));
	}
}
