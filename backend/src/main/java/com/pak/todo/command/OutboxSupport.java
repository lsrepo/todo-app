package com.pak.todo.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pak.todo.model.entity.OutboxEntry;
import com.pak.todo.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxSupport {

	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	public void saveOutbox(String aggregateType, String aggregateId, String eventType, Object payload) {
		try {
			String payloadJson = objectMapper.writeValueAsString(payload);
			OutboxEntry entry = OutboxEntry.builder()
					.id(UUID.randomUUID())
					.aggregateType(aggregateType)
					.aggregateId(aggregateId)
					.eventType(eventType)
					.payload(payloadJson)
					.createdAt(Instant.now())
					.build();
			outboxRepository.save(entry);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize outbox payload", e);
		}
	}
}
