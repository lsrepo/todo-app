package com.pak.todo.outbox;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;

import com.pak.todo.model.entity.OutboxEntry;
import com.pak.todo.websocket.WebSocketBroadcaster;

class OutboxKafkaConsumerTest {

	private WebSocketBroadcaster webSocketBroadcaster;
	private OutboxKafkaConsumer consumer;

	@BeforeEach
	void setUp() {
		webSocketBroadcaster = Mockito.mock(WebSocketBroadcaster.class);
		consumer = new OutboxKafkaConsumer(new com.fasterxml.jackson.databind.ObjectMapper(), webSocketBroadcaster);
	}

	private String debeziumOutboxValue(UUID boardId, String aggregateType, String aggregateId, String eventType, String payload) {
		return """
				{"after":{"id":"%s","board_id":"%s","aggregate_type":"%s","aggregate_id":"%s","event_type":"%s","payload":"%s"}}
				""".formatted(
				UUID.randomUUID(),
				boardId,
				aggregateType,
				aggregateId,
				eventType,
				payload.replace("\"", "\\\"")
		).trim();
	}

	// Scenario: Debezium outbox record is parsed and broadcast to the correct board
	// Given: a ConsumerRecord with valid Debezium "after" payload for board A
	// When: onMessage() is called
	// Then: broadcaster.broadcast() is invoked with that boardId and an OutboxEntry with correct fields
	@Test
	void onMessage_validDebeziumPayload_broadcastsToBoard() {
		UUID boardA = UUID.randomUUID();
		String payload = "{\"name\":\"Task one\",\"status\":\"IN_PROGRESS\"}";
		String json = debeziumOutboxValue(boardA, "Task", "task-1", "TaskUpdated", payload);
		ConsumerRecord<String, String> record = new ConsumerRecord<>("debezium.public.outbox", 0, 0L, "key", json);

		consumer.onMessage(record);

		ArgumentCaptor<OutboxEntry> entryCaptor = ArgumentCaptor.forClass(OutboxEntry.class);
		verify(webSocketBroadcaster).broadcast(eq(boardA), entryCaptor.capture());
		OutboxEntry entry = entryCaptor.getValue();
		assertThat(entry.getBoardId()).isEqualTo(boardA);
		assertThat(entry.getAggregateType()).isEqualTo("Task");
		assertThat(entry.getAggregateId()).isEqualTo("task-1");
		assertThat(entry.getEventType()).isEqualTo("TaskUpdated");
		assertThat(entry.getPayload()).isEqualTo(payload);
	}

	// Scenario: Event without board_id is ignored (no broadcast)
	// Given: a ConsumerRecord with "after" payload missing board_id
	// When: onMessage() is called
	// Then: broadcaster is never called
	@Test
	void onMessage_noBoardId_doesNotBroadcast() {
		String json = """
				{"after":{"id":"%s","aggregate_type":"Task","aggregate_id":"t1","event_type":"TaskUpdated","payload":"{}"}}
				""".formatted(UUID.randomUUID()).trim();
		ConsumerRecord<String, String> record = new ConsumerRecord<>("debezium.public.outbox", 0, 0L, "key", json);

		consumer.onMessage(record);

		Mockito.verifyNoInteractions(webSocketBroadcaster);
	}

	// Scenario: Malformed or non-JSON value does not throw; no broadcast
	// Given: a ConsumerRecord with invalid JSON value
	// When: onMessage() is called
	// Then: no exception is thrown and broadcaster is not called
	@Test
	void onMessage_invalidJson_doesNotThrowAndDoesNotBroadcast() {
		ConsumerRecord<String, String> record = new ConsumerRecord<>("debezium.public.outbox", 0, 0L, "key", "not json");

		consumer.onMessage(record);

		Mockito.verifyNoInteractions(webSocketBroadcaster);
	}

	// Scenario: Null value is ignored
	// Given: a ConsumerRecord with null value
	// When: onMessage() is called
	// Then: broadcaster is not called
	@Test
	void onMessage_nullValue_doesNotBroadcast() {
		ConsumerRecord<String, String> record = new ConsumerRecord<>("debezium.public.outbox", 0, 0L, "key", null);

		consumer.onMessage(record);

		Mockito.verifyNoInteractions(webSocketBroadcaster);
	}
}
