package com.pak.todo.websocket;

import java.net.URI;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.pak.todo.auth.AuthorizationService;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.User;
import com.pak.todo.service.BoardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoardWebSocketHandler extends TextWebSocketHandler {

	private final WebSocketBroadcaster webSocketBroadcaster;
	private final BoardService boardService;
	private final AuthorizationService authorizationService;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		UUID boardId = extractBoardId(session.getUri());
		if (boardId == null) {
			log.warn("WebSocket connection missing or invalid boardId in path: {}", session.getUri());
			session.close(CloseStatus.BAD_DATA);
			return;
		}

		Object principal = session.getAttributes().get("user");
		if (!(principal instanceof User user)) {
			log.warn("WebSocket connection missing authenticated user for board {}", boardId);
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
			return;
		}

		Board board = boardService.getEntityById(boardId);
		if (board == null) {
			log.warn("WebSocket connection for non-existent board {}", boardId);
			session.close(CloseStatus.BAD_DATA);
			return;
		}

		if (!authorizationService.canViewBoard(user, board)) {
			log.warn("WebSocket connection forbidden for user {} on board {}", user.getId(), boardId);
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Forbidden"));
			return;
		}

		webSocketBroadcaster.register(boardId, session);
		log.info("WebSocket session {} registered for board {}", session.getId(), boardId);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		webSocketBroadcaster.unregister(session);
		log.info("WebSocket session {} closed with status {}", session.getId(), status);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		log.warn("WebSocket transport error for session {}", session != null ? session.getId() : "unknown", exception);
		if (session != null) {
			webSocketBroadcaster.unregister(session);
		}
	}

	private UUID extractBoardId(URI uri) {
		if (uri == null) {
			return null;
		}
		String path = uri.getPath(); // e.g. /ws/board/{boardId}
		if (path == null) {
			return null;
		}
		String[] segments = path.split("/");
		if (segments.length < 4) {
			return null;
		}
		String idSegment = segments[segments.length - 1];
		try {
			return UUID.fromString(idSegment);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}
}

