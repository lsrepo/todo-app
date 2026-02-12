package com.pak.todo.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
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
		HttpHeaders headers = request.getHeaders();

		// 1. Try standard Authorization header first.
		String authHeader = headers.getFirst("Authorization");
		String token = null;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
		}

		// 2. If no valid Authorization header, fall back to Sec-WebSocket-Protocol.
		if (token == null) {
			List<String> protocolHeaders = headers.get("Sec-WebSocket-Protocol");
			if (protocolHeaders != null && !protocolHeaders.isEmpty()) {
				List<String> protocols = new ArrayList<>();
				for (String headerValue : protocolHeaders) {
					if (headerValue == null) {
						continue;
					}
					// Header may contain a comma-separated list; split and trim.
					for (String part : headerValue.split(",")) {
						String trimmed = part.trim();
						if (!trimmed.isEmpty()) {
							protocols.add(trimmed);
						}
					}
				}

				if (protocols.size() >= 2) {
					// First entry is the real protocol (e.g. board-v1), second is the JWT token.
					token = protocols.get(1);
				}
			}
		}

		if (token == null) {
			log.warn("WebSocket handshake missing valid Authorization header and JWT in Sec-WebSocket-Protocol");
			return false;
		}

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

