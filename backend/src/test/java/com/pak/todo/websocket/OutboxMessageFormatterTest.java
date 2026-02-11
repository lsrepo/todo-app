package com.pak.todo.websocket;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pak.todo.model.entity.OutboxEntry;

class OutboxMessageFormatterTest {

	private OutboxMessageFormatter formatter;

	@BeforeEach
	void setUp() {
		formatter = new OutboxMessageFormatter(new ObjectMapper());
	}

	private OutboxEntry entry(String aggregateType, String aggregateId, String eventType, String payload) {
		return OutboxEntry.builder()
				.id(UUID.randomUUID())
				.aggregateType(aggregateType)
				.aggregateId(aggregateId)
				.boardId(UUID.randomUUID())
				.eventType(eventType)
				.payload(payload != null ? payload : "{}")
				.createdAt(Instant.now())
				.build();
	}

	// Scenario: task updated with status in payload
	// Given: an OutboxEntry for Task with event_type TaskUpdated and payload containing status
	// When: format() is called
	// Then: message contains type=edit, resource=task, key=status, and value equals payload status
	@Test
	void format_taskUpdated_withStatus_usesStatusKeyAndValue() {
		OutboxEntry entry = entry("Task", "task-123", "TaskUpdated",
				"{\"name\":\"Task name\",\"status\":\"IN_PROGRESS\"}");

		String result = formatter.format(entry);

		assertThat(result).contains("type=edit");
		assertThat(result).contains("resource=task");
		assertThat(result).contains("id=task-123");
		assertThat(result).contains("key=status");
		assertThat(result).contains("value=IN_PROGRESS");
	}

	// Scenario: task updated without status in payload
	// Given: an OutboxEntry for Task with payload containing name but no status
	// When: format() is called
	// Then: message uses key=name and value equals payload name
	@Test
	void format_taskUpdated_withoutStatus_usesNameKeyAndValue() {
		OutboxEntry entry = entry("Task", "task-456", "TaskUpdated",
				"{\"name\":\"My task name\",\"description\":\"desc\"}");

		String result = formatter.format(entry);

		assertThat(result).contains("type=edit");
		assertThat(result).contains("resource=task");
		assertThat(result).contains("id=task-456");
		assertThat(result).contains("key=name");
		assertThat(result).contains("value=My task name");
	}

	// Scenario: board updated uses name
	// Given: an OutboxEntry for Board with event_type BoardUpdated and payload containing name
	// When: format() is called
	// Then: message contains resource=board, key=name, and value equals payload name
	@Test
	void format_boardUpdated_usesNameKeyAndValue() {
		OutboxEntry entry = entry("Board", "board-789", "BoardUpdated",
				"{\"name\":\"Board title\",\"description\":\"board desc\"}");

		String result = formatter.format(entry);

		assertThat(result).contains("type=edit");
		assertThat(result).contains("resource=board");
		assertThat(result).contains("id=board-789");
		assertThat(result).contains("key=name");
		assertThat(result).contains("value=Board title");
	}

	// Scenario: event type mapping - created -> create
	@Test
	void format_eventTypeCreated_mapsToCreate() {
		OutboxEntry entry = entry("Task", "t1", "TaskCreated", "{\"name\":\"x\"}");
		String result = formatter.format(entry);
		assertThat(result).contains("type=create");
		assertThat(result).contains("resource=task");
	}

	// Scenario: event type mapping - deleted -> delete
	@Test
	void format_eventTypeDeleted_mapsToDelete() {
		OutboxEntry entry = entry("Task", "t2", "TaskDeleted", "{}");
		String result = formatter.format(entry);
		assertThat(result).contains("type=delete");
		assertThat(result).contains("resource=task");
	}

	// Scenario: event type null maps to edit
	@Test
	void format_eventTypeNull_mapsToEdit() {
		OutboxEntry entry = OutboxEntry.builder()
				.id(UUID.randomUUID())
				.aggregateType("Task")
				.aggregateId("t3")
				.boardId(UUID.randomUUID())
				.eventType(null)
				.payload("{\"name\":\"y\"}")
				.createdAt(Instant.now())
				.build();
		String result = formatter.format(entry);
		assertThat(result).contains("type=edit");
	}

	// Scenario: value with semicolon is escaped
	@Test
	void format_valueWithSemicolon_escaped() {
		OutboxEntry entry = entry("Board", "b1", "BoardUpdated", "{\"name\":\"a;b;c\"}");
		String result = formatter.format(entry);
		assertThat(result).contains("value=a\\;b\\;c");
	}

	// Scenario: value with equals is escaped
	@Test
	void format_valueWithEquals_escaped() {
		OutboxEntry entry = entry("Board", "b2", "BoardUpdated", "{\"name\":\"key=value\"}");
		String result = formatter.format(entry);
		assertThat(result).contains("value=key\\=value");
	}

	// Scenario: invalid payload falls back to minimal message
	@Test
	void format_invalidPayload_returnsMinimalMessage() {
		OutboxEntry entry = entry("Task", "t4", "TaskUpdated", "not json");
		String result = formatter.format(entry);
		assertThat(result).contains("type=edit");
		assertThat(result).contains("resource=task");
		assertThat(result).contains("id=t4");
		assertThat(result).contains("key=all");
		assertThat(result).contains("value=");
	}

	// Scenario: aggregate type case-insensitive for task
	@Test
	void format_aggregateTypeTaskUpperCase_resourceIsTask() {
		OutboxEntry entry = entry("TASK", "id", "TaskUpdated", "{\"name\":\"x\"}");
		String result = formatter.format(entry);
		assertThat(result).contains("resource=task");
	}
}
