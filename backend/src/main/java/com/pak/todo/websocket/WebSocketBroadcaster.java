package com.pak.todo.websocket;

import com.pak.todo.model.entity.OutboxEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketBroadcaster {

	private final OutboxMessageFormatter outboxMessageFormatter;

	private final Map<UUID, Set<WebSocketSession>> sessionsByBoard = new ConcurrentHashMap<>();
	private final Map<String, UUID> boardBySessionId = new ConcurrentHashMap<>();

	public void register(UUID boardId, WebSocketSession session) {
		sessionsByBoard.computeIfAbsent(boardId, id -> ConcurrentHashMap.newKeySet()).add(session);
		boardBySessionId.put(session.getId(), boardId);
	}

	public void unregister(WebSocketSession session) {
		UUID boardId = boardBySessionId.remove(session.getId());
		if (boardId == null) {
			return;
		}
		Set<WebSocketSession> sessions = sessionsByBoard.get(boardId);
		if (sessions != null) {
			sessions.remove(session);
			if (sessions.isEmpty()) {
				sessionsByBoard.remove(boardId);
			}
		}
	}

	public void broadcast(UUID boardId, OutboxEntry entry) {
		String message = outboxMessageFormatter.format(entry);
		Set<WebSocketSession> sessions = sessionsByBoard.get(boardId);
		if (sessions == null || sessions.isEmpty()) {
			log.info("No WebSocket session for board {}, skipping broadcast", boardId);
			return;
		}

		TextMessage textMessage = new TextMessage(message);

		for (WebSocketSession session : sessions) {
			if (!session.isOpen()) {
				continue;
			}
			try {
				session.sendMessage(textMessage);
				log.info("Broadcast WebSocket message to session {} (board {}): {}", session.getId(), boardId, message);
			}
			catch (IOException e) {
				log.warn("Failed to send WebSocket message to session {}", session.getId(), e);
			}
		}
	}

}

