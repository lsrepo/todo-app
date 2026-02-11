package com.pak.todo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.pak.todo.websocket.BoardWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

	private final BoardWebSocketHandler boardWebSocketHandler;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// Clients connect to ws://<host>:<port>/ws/board/{boardId}
		registry.addHandler(boardWebSocketHandler, "/ws/board/*")
				.setAllowedOrigins("*");
	}
}

