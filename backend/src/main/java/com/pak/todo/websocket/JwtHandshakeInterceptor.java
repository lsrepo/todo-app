package com.pak.todo.websocket;

import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.pak.todo.repository.UserRepository;
import com.pak.todo.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	@Override
	public boolean beforeHandshake(
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSocketHandler wsHandler,
			Map<String, Object> attributes
	) {
		List<String> authHeaders = request.getHeaders().get("Authorization");
		if (authHeaders == null || authHeaders.isEmpty()) {
			log.warn("WebSocket handshake missing Authorization header");
			return false;
		}

		String header = authHeaders.getFirst();
		if (header == null || !header.startsWith("Bearer ")) {
			log.warn("WebSocket handshake has invalid Authorization header");
			return false;
		}

		String token = header.substring(7);

		return jwtService.parseAndValidate(token)
				.flatMap(principal -> userRepository.findById(principal.userId()))
				.map(user -> {
					attributes.put("user", user);
					return true;
				})
				.orElseGet(() -> {
					log.warn("WebSocket handshake JWT invalid or user not found");
					return false;
				});
	}

	@Override
	public void afterHandshake(
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSocketHandler wsHandler,
			Exception exception
	) {
		// no-op
	}
}

