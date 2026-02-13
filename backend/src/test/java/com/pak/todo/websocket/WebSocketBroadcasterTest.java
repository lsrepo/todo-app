package com.pak.todo.websocket;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.pak.todo.model.entity.OutboxEntry;

class WebSocketBroadcasterTest {

	private OutboxMessageFormatter formatter;
	private WebSocketBroadcaster broadcaster;

	@BeforeEach
	void setUp() {
		formatter = new OutboxMessageFormatter(new com.fasterxml.jackson.databind.ObjectMapper());
		broadcaster = new WebSocketBroadcaster(formatter);
	}

	private OutboxEntry entry(UUID boardId, String aggregateType, String aggregateId, String eventType, String payload) {
		return OutboxEntry.builder()
				.id(UUID.randomUUID())
				.aggregateType(aggregateType)
				.aggregateId(aggregateId)
				.boardId(boardId)
				.eventType(eventType)
				.payload(payload != null ? payload : "{}")
				.createdAt(Instant.now())
				.build();
	}

	private WebSocketSession openSession(String id) {
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getId()).thenReturn(id);
		when(session.isOpen()).thenReturn(true);
		return session;
	}

	private WebSocketSession closedSession(String id) {
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getId()).thenReturn(id);
		when(session.isOpen()).thenReturn(false);
		return session;
	}

	// Scenario: broadcasting to a board with multiple sessions sends message to all
	// Given: two sessions registered for the same board
	// When: broadcast(boardId, entry) is called
	// Then: both sessions receive the formatted message
	@Test
	void broadcast_multipleSessionsOnSameBoard_sendsToAll() throws IOException {
		UUID boardA = UUID.randomUUID();
		WebSocketSession session1 = openSession("s1");
		WebSocketSession session2 = openSession("s2");
		broadcaster.register(boardA, session1);
		broadcaster.register(boardA, session2);
		OutboxEntry entry = entry(boardA, "Task", "task-1", "TaskUpdated", "{\"name\":\"x\"}");
		broadcaster.broadcast(boardA, entry);
		verify(session1).sendMessage(any(TextMessage.class));
		verify(session2).sendMessage(any(TextMessage.class));
	}

	// Scenario: broadcasting to one board does not send to sessions on another board
	// Given: sessions registered for board A and board B
	// When: broadcast(boardA, entry) is called
	// Then: only board A sessions receive the message
	@Test
	void broadcast_differentBoards_onlyBoardASessionsReceive() throws IOException {
		UUID boardA = UUID.randomUUID();
		UUID boardB = UUID.randomUUID();
		WebSocketSession client1 = openSession("c1");
		WebSocketSession client2 = openSession("c2");
		WebSocketSession client3 = openSession("c3");
		broadcaster.register(boardA, client1);
		broadcaster.register(boardA, client2);
		broadcaster.register(boardB, client3);
		OutboxEntry entry = entry(boardA, "Task", "t1", "TaskUpdated", "{\"name\":\"y\"}");
		broadcaster.broadcast(boardA, entry);
		verify(client1).sendMessage(any(TextMessage.class));
		verify(client2).sendMessage(any(TextMessage.class));
		verify(client3, never()).sendMessage(any(TextMessage.class));
	}

	// Scenario: closed sessions are skipped and do not receive messages
	// Given: one open and one closed session registered for the same board
	// When: broadcast is called
	// Then: only the open session receives the message
	@Test
	void broadcast_closedSession_skipped() throws IOException {
		UUID boardId = UUID.randomUUID();
		WebSocketSession open = openSession("open");
		WebSocketSession closed = closedSession("closed");
		broadcaster.register(boardId, open);
		broadcaster.register(boardId, closed);
		OutboxEntry entry = entry(boardId, "Board", "b1", "BoardUpdated", "{\"name\":\"z\"}");
		broadcaster.broadcast(boardId, entry);
		verify(open).sendMessage(any(TextMessage.class));
		verify(closed, never()).sendMessage(any(TextMessage.class));
	}

	// Scenario: after unregistering a session only remaining sessions receive broadcast
	// Given: two sessions registered, then one unregistered
	// When: broadcast is called
	// Then: only the still-registered session receives the message
	@Test
	void broadcast_afterUnregister_onlyRemainingSessionReceives() throws IOException {
		UUID boardA = UUID.randomUUID();
		WebSocketSession session1 = openSession("s1");
		WebSocketSession session2 = openSession("s2");
		broadcaster.register(boardA, session1);
		broadcaster.register(boardA, session2);
		broadcaster.unregister(session2);
		OutboxEntry entry = entry(boardA, "Task", "t1", "TaskUpdated", "{\"name\":\"w\"}");
		broadcaster.broadcast(boardA, entry);
		verify(session1).sendMessage(any(TextMessage.class));
		verify(session2, never()).sendMessage(any(TextMessage.class));
	}

	// Scenario: unregistering an unknown session does not throw
	// Given: a session that was never registered
	// When: unregister(session) and then broadcast for another board are called
	// Then: no exception is thrown and the session receives nothing
	@Test
	void unregister_unknownSession_doesNotThrow() throws IOException {
		WebSocketSession session = openSession("orphan");
		broadcaster.unregister(session);
		UUID boardId = UUID.randomUUID();
		OutboxEntry entry = entry(boardId, "Task", "t1", "TaskUpdated", "{}");
		broadcaster.broadcast(boardId, entry);
		verify(session, never()).sendMessage(any(TextMessage.class));
	}

	// Scenario: broadcasting to a board with no registered sessions does nothing
	// Given: sessions registered for a different board only
	// When: broadcast(targetBoardId, entry) is called
	// Then: no session receives a message
	@Test
	void broadcast_noSessionsForBoard_doesNothing() throws IOException {
		UUID boardA = UUID.randomUUID();
		WebSocketSession session = openSession("s1");
		broadcaster.register(UUID.randomUUID(), session);
		OutboxEntry entry = entry(boardA, "Task", "t1", "TaskUpdated", "{}");
		broadcaster.broadcast(boardA, entry);
		verify(session, never()).sendMessage(any(TextMessage.class));
	}

	// Scenario: broadcast message content uses formatter output with type, resource, id, key, value
	// Given: an outbox entry with task updated and status in payload
	// When: broadcast is called
	// Then: the session receives a TextMessage whose payload contains the expected key-value format
	@Test
	void broadcast_messageContent_containsExpectedFormat() throws IOException {
		UUID boardA = UUID.randomUUID();
		WebSocketSession session = openSession("s1");
		broadcaster.register(boardA, session);
		OutboxEntry entry = entry(boardA, "Task", "task-99", "TaskUpdated",
				"{\"name\":\"n\",\"status\":\"IN_PROGRESS\"}");
		broadcaster.broadcast(boardA, entry);
		var captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
		verify(session).sendMessage(captor.capture());
		String payload = captor.getValue().getPayload();
		assertThat(payload).contains("type=edit");
		assertThat(payload).contains("resource=task");
		assertThat(payload).contains("id=task-99");
		assertThat(payload).contains("key=status");
		assertThat(payload).contains("value=IN_PROGRESS");
	}

	// Scenario: when one session throws IOException on sendMessage others still receive and no exception propagates
	// Given: two sessions registered for the same board, first session throws IOException on sendMessage
	// When: broadcast is called
	// Then: the other session receives the message and no exception is thrown
	@Test
	void broadcast_sendThrowsIOException_continuesToOtherSessions() throws IOException {
		UUID boardId = UUID.randomUUID();
		WebSocketSession failingSession = openSession("fail");
		WebSocketSession okSession = openSession("ok");
		doThrow(new IOException("send failed")).when(failingSession).sendMessage(any(TextMessage.class));
		broadcaster.register(boardId, failingSession);
		broadcaster.register(boardId, okSession);
		OutboxEntry entry = entry(boardId, "Task", "t1", "TaskUpdated", "{\"name\":\"x\"}");

		broadcaster.broadcast(boardId, entry);

		verify(okSession).sendMessage(any(TextMessage.class));
		verify(failingSession).sendMessage(any(TextMessage.class));
	}
}
