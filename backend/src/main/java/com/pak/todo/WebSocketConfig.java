package com.pak.todo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.pak.todo.websocket.BoardWebSocketHandler;
import com.pak.todo.websocket.JwtHandshakeInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

	private final BoardWebSocketHandler boardWebSocketHandler;
	private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
		handshakeHandler.setSupportedProtocols("board-v1");

		// Clients connect to ws://<host>:<port>/ws/board/{boardId}
		registry.addHandler(boardWebSocketHandler, "/ws/board/*")
				.setHandshakeHandler(handshakeHandler)
				.addInterceptors(jwtHandshakeInterceptor)
				.setAllowedOrigins("*");
	}
}

