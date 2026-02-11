package com.pak.todo.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pak.todo.model.entity.OutboxEntry;
import com.pak.todo.websocket.WebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final WebSocketBroadcaster webSocketBroadcaster;

	@KafkaListener(topics = "debezium.public.outbox", groupId = "outbox-logger")
	public void onMessage(ConsumerRecord<String, String> record) {
		String value = record.value();
		if (value == null) {
			log.warn("Received null value from topic {}", record.topic());
			return;
		}

		try {
			JsonNode root = objectMapper.readTree(value);
			JsonNode payloadNode = root.has("payload") ? root.get("payload") : root;
			JsonNode afterNode = payloadNode.has("after") ? payloadNode.get("after") : payloadNode;

			// Extract board_id (mandatory for board/task events)
			JsonNode boardIdNode = afterNode.get("board_id");
			if (boardIdNode == null || boardIdNode.isNull()) {
				log.debug("Outbox event without board_id, skipping WebSocket broadcast. Payload: {}", payloadNode);
				return;
			}

			UUID boardId = UUID.fromString(boardIdNode.asText());

			OutboxEntry entry = OutboxEntry.builder()
					.id(parseUuid(afterNode.get("id")))
					.aggregateType(asText(afterNode, "aggregate_type"))
					.aggregateId(asText(afterNode, "aggregate_id"))
					.boardId(boardId)
					.eventType(asText(afterNode, "event_type"))
					.payload(asText(afterNode, "payload"))
					.build();

			webSocketBroadcaster.broadcast(boardId, entry);

			// Keep logging for observability
			log.info("Outbox payload broadcast for board {}: {}", boardId, entry.getPayload());
		} catch (JsonProcessingException e) {
			// If parsing fails (e.g. non-JSON), log the raw value so nothing is lost
			log.warn("Failed to parse outbox message as JSON, logging raw value. Error: {}", e.getMessage());
			log.info("Outbox payload (raw): {}", value);
		} catch (Exception e) {
			log.warn("Failed to process outbox message for WebSocket broadcast", e);
		}
	}

	private UUID parseUuid(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		try {
			return UUID.fromString(node.asText());
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private String asText(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return field != null && !field.isNull() ? field.asText() : null;
	}
}

