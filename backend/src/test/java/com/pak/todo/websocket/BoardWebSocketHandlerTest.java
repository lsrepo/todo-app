package com.pak.todo.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.pak.todo.auth.AuthorizationService;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.User;
import com.pak.todo.service.BoardService;

class BoardWebSocketHandlerTest {

	private WebSocketBroadcaster webSocketBroadcaster;
	private BoardService boardService;
	private AuthorizationService authorizationService;
	private BoardWebSocketHandler handler;

	@BeforeEach
	void setUp() {
		webSocketBroadcaster = mock(WebSocketBroadcaster.class);
		boardService = mock(BoardService.class);
		authorizationService = mock(AuthorizationService.class);
		handler = new BoardWebSocketHandler(webSocketBroadcaster, boardService, authorizationService);
	}

	private WebSocketSession sessionWithUriAndUser(URI uri, User user) {
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getUri()).thenReturn(uri);
		Map<String, Object> attributes = new HashMap<>();
		if (user != null) {
			attributes.put("user", user);
		}
		when(session.getAttributes()).thenReturn(attributes);
		when(session.getId()).thenReturn("session-1");
		return session;
	}

	// Scenario: connection with valid board path and user who can view registers with broadcaster
	// Given: session URI /ws/board/{boardId}, attributes contain User, board exists, user can view
	// When: afterConnectionEstablished(session) is called
	// Then: webSocketBroadcaster.register(boardId, session) is invoked
	@Test
	void afterConnectionEstablished_validBoardAndUser_registersWithBroadcaster() throws Exception {
		UUID boardId = UUID.randomUUID();
		URI uri = URI.create("http://localhost/ws/board/" + boardId);
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		WebSocketSession session = sessionWithUriAndUser(uri, user);

		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(authorizationService.canViewBoard(user, board)).thenReturn(true);

		handler.afterConnectionEstablished(session);

		verify(webSocketBroadcaster).register(eq(boardId), eq(session));
	}

	// Scenario: connection with invalid or missing boardId in path closes with BAD_DATA
	// Given: session URI with too few path segments (e.g. /ws/board)
	// When: afterConnectionEstablished(session) is called
	// Then: session.close(CloseStatus.BAD_DATA) is invoked
	@Test
	void afterConnectionEstablished_invalidBoardId_closesWithBadData() throws Exception {
		URI uri = URI.create("http://localhost/ws/board");
		User user = User.create(UUID.randomUUID(), "user", "hash");
		WebSocketSession session = sessionWithUriAndUser(uri, user);

		handler.afterConnectionEstablished(session);

		verify(session).close(CloseStatus.BAD_DATA);
		verify(webSocketBroadcaster, never()).register(any(), any());
	}

	// Scenario: connection with null URI closes with BAD_DATA
	@Test
	void afterConnectionEstablished_nullUri_closesWithBadData() throws Exception {
		WebSocketSession session = sessionWithUriAndUser(null, User.create(UUID.randomUUID(), "u", "h"));

		handler.afterConnectionEstablished(session);

		verify(session).close(CloseStatus.BAD_DATA);
	}

	// Scenario: connection when user is missing in attributes closes with NOT_ACCEPTABLE
	// Given: session has valid board URI but attributes do not contain a User
	// When: afterConnectionEstablished(session) is called
	// Then: session.close(CloseStatus.NOT_ACCEPTABLE with reason "Unauthorized") is invoked
	@Test
	void afterConnectionEstablished_noUserInAttributes_closesWithNotAcceptable() throws Exception {
		UUID boardId = UUID.randomUUID();
		URI uri = URI.create("http://localhost/ws/board/" + boardId);
		WebSocketSession session = sessionWithUriAndUser(uri, null);

		handler.afterConnectionEstablished(session);

		verify(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
		verify(webSocketBroadcaster, never()).register(any(), any());
	}

	// Scenario: connection for non-existent board closes with BAD_DATA
	// Given: session has valid board URI and user but BoardService returns null for board
	// When: afterConnectionEstablished(session) is called
	// Then: session.close(CloseStatus.BAD_DATA) is invoked
	@Test
	void afterConnectionEstablished_boardNotFound_closesWithBadData() throws Exception {
		UUID boardId = UUID.randomUUID();
		URI uri = URI.create("http://localhost/ws/board/" + boardId);
		User user = User.create(UUID.randomUUID(), "user", "hash");
		WebSocketSession session = sessionWithUriAndUser(uri, user);

		when(boardService.getEntityById(boardId)).thenReturn(null);

		handler.afterConnectionEstablished(session);

		verify(session).close(CloseStatus.BAD_DATA);
		verify(webSocketBroadcaster, never()).register(any(), any());
	}

	// Scenario: connection when user cannot view board closes with NOT_ACCEPTABLE Forbidden
	// Given: board exists but authorizationService.canViewBoard returns false
	// When: afterConnectionEstablished(session) is called
	// Then: session.close(CloseStatus.NOT_ACCEPTABLE with reason "Forbidden") is invoked
	@Test
	void afterConnectionEstablished_userCannotViewBoard_closesWithNotAcceptable() throws Exception {
		UUID boardId = UUID.randomUUID();
		URI uri = URI.create("http://localhost/ws/board/" + boardId);
		User user = User.create(UUID.randomUUID(), "user", "hash");
		Board board = Board.create(boardId, "Board", "Desc");
		WebSocketSession session = sessionWithUriAndUser(uri, user);

		when(boardService.getEntityById(boardId)).thenReturn(board);
		when(authorizationService.canViewBoard(user, board)).thenReturn(false);

		handler.afterConnectionEstablished(session);

		verify(session).close(CloseStatus.NOT_ACCEPTABLE.withReason("Forbidden"));
		verify(webSocketBroadcaster, never()).register(any(), any());
	}

	// Scenario: after connection closed unregisters session from broadcaster
	// Given: a WebSocket session
	// When: afterConnectionClosed(session, status) is called
	// Then: webSocketBroadcaster.unregister(session) is invoked
	@Test
	void afterConnectionClosed_callsUnregister() throws Exception {
		WebSocketSession session = mock(WebSocketSession.class);
		CloseStatus status = CloseStatus.NORMAL;

		handler.afterConnectionClosed(session, status);

		verify(webSocketBroadcaster).unregister(session);
	}

	// Scenario: transport error unregisters session when session is non-null
	// Given: a WebSocket session and a throwable
	// When: handleTransportError(session, exception) is called
	// Then: webSocketBroadcaster.unregister(session) is invoked
	@Test
	void handleTransportError_unregistersSession() throws Exception {
		WebSocketSession session = mock(WebSocketSession.class);
		Throwable exception = new RuntimeException("transport error");

		handler.handleTransportError(session, exception);

		verify(webSocketBroadcaster).unregister(session);
	}

	// Scenario: transport error when session is null does not throw and does not call unregister with null
	// Given: session is null
	// When: handleTransportError(null, exception) is called
	// Then: no exception is thrown and unregister is not called (or would be safe)
	@Test
	void handleTransportError_sessionNull_doesNotThrow() throws Exception {
		handler.handleTransportError(null, new RuntimeException("error"));
		verify(webSocketBroadcaster, never()).unregister(any());
	}
}
