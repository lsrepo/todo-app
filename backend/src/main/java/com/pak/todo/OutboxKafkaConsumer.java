package com.pak.todo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxKafkaConsumer {

	private final ObjectMapper objectMapper;

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
			log.info("Outbox payload: {}", payloadNode.toString());
		} catch (JsonProcessingException e) {
			// If parsing fails (e.g. non-JSON), log the raw value so nothing is lost
			log.warn("Failed to parse outbox message as JSON, logging raw value. Error: {}", e.getMessage());
			log.info("Outbox payload (raw): {}", value);
		}
	}
}

