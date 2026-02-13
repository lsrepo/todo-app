package com.pak.todo.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pak.todo.model.entity.OutboxEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxMessageFormatter {

	private final ObjectMapper objectMapper;

	public String format(OutboxEntry entry) {
		try {
			JsonNode payloadNode = objectMapper.readTree(entry.getPayload());

			String resource = "Task".equalsIgnoreCase(entry.getAggregateType()) ? "task" : "board";
			String type = mapEventType(entry.getEventType());
			String id = entry.getAggregateId();
			String key;
			String value;

			if ("task".equals(resource)) {
				JsonNode statusNode = payloadNode.get("status");
				if (statusNode != null && !statusNode.isNull()) {
					key = "status";
					value = statusNode.asText("");
				}
				else {
					JsonNode nameNode = payloadNode.get("name");
					if (nameNode != null && !nameNode.isNull()) {
						key = "name";
						value = nameNode.asText("");
					}
					else {
						JsonNode dueDateNode = payloadNode.get("dueDate");
						if (dueDateNode != null && !dueDateNode.isNull()) {
							key = "dueDate";
							value = dueDateNode.asText("");
						}
						else {
							JsonNode descNode = payloadNode.get("description");
							key = "description";
							value = descNode != null ? descNode.asText("") : "";
						}
					}
				}
			}
			else {
				JsonNode nameNode = payloadNode.get("name");
				key = "name";
				value = nameNode != null ? nameNode.asText("") : "";
			}

			return "type=" + type +
					";resource=" + resource +
					";id=" + (id != null ? id : "") +
					";key=" + key +
					";value=" + escape(value);
		}
		catch (IOException e) {
			log.warn("IO error while transforming outbox entry payload, falling back to minimal message", e);
		}
		catch (RuntimeException e) {
			log.warn("Failed to transform outbox entry payload, falling back to minimal message", e);
			String resource = "Task".equalsIgnoreCase(entry.getAggregateType()) ? "task" : "board";
			String type = mapEventType(entry.getEventType());
			return "type=" + type +
					";resource=" + resource +
					";id=" + (entry.getAggregateId() != null ? entry.getAggregateId() : "") +
					";key=all;value=";
		}
		String resource = "Task".equalsIgnoreCase(entry.getAggregateType()) ? "task" : "board";
		String type = mapEventType(entry.getEventType());
		return "type=" + type +
				";resource=" + resource +
				";id=" + (entry.getAggregateId() != null ? entry.getAggregateId() : "") +
				";key=all;value=";
	}

	private String escape(String value) {
		return value
				.replace(";", "\\;")
				.replace("=", "\\=");
	}

	private String mapEventType(String eventType) {
		if (eventType == null) {
			return "edit";
		}
		String lower = eventType.toLowerCase(Locale.ROOT);
		if (lower.contains("created")) {
			return "create";
		}
		if (lower.contains("deleted")) {
			return "delete";
		}
		return "edit";
	}
}

