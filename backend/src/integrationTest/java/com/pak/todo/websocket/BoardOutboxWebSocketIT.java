package com.pak.todo.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pak.todo.model.dto.LoginRequest;
import com.pak.todo.model.dto.LoginResponse;
import com.pak.todo.model.dto.TaskCreateRequest;
import com.pak.todo.model.dto.TaskResponse;
import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.DebeziumContainer;
import io.strimzi.test.container.StrimziKafkaCluster;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.lifecycle.Startables.deepStart;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BoardOutboxWebSocketIT {

	private static final Network NETWORK = Network.SHARED;

	private static final String SEED_USERNAME = "user1";
	private static final String SEED_PASSWORD = "password";
	private static final String SEED_BOARD_NAME = "user1's board";

	private static final int WEBSOCKET_HANDSHAKE_TIMEOUT_SEC = 30;
	private static final int DEBEZIUM_WARMUP_MS = 5_000;
	private static final int OUTBOX_MESSAGE_TIMEOUT_SEC = 10;

	static final StrimziKafkaCluster kafkaContainer = new StrimziKafkaCluster.StrimziKafkaClusterBuilder()
			.withNumberOfBrokers(1)
			.withSharedNetwork()
			.build();

	@Container
	static PostgreSQLContainer<?> postgresContainer =
			new PostgreSQLContainer<>(DockerImageName.parse("postgres:17").asCompatibleSubstituteFor("postgres"))
					.withNetwork(NETWORK)
					.withNetworkAliases("postgres")
					.withCommand(
							"postgres",
							"-c", "wal_level=logical",
							"-c", "max_wal_senders=10",
							"-c", "max_replication_slots=10"
					);

	@Container
	static DebeziumContainer debeziumContainer = new DebeziumContainer("quay.io/debezium/connect:3.4.1.Final")
			.withNetwork(NETWORK)
			.withKafka(kafkaContainer)
			.dependsOn(kafkaContainer, postgresContainer);

	@BeforeAll
	static void startContainers() {
		deepStart(Stream.of(kafkaContainer, postgresContainer, debeziumContainer)).join();
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
		registry.add("spring.datasource.username", postgresContainer::getUsername);
		registry.add("spring.datasource.password", postgresContainer::getPassword);
		registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
	}

	// Scenario: creating a task on a board notifies subscribed clients via WebSocket outbox
	// Given: a logged-in user with a board, subscribed to that board over WebSocket, and CDC pipeline ready
	// When: the user creates a new task on the board via the REST API
	// Then: a WebSocket message is received with the task create event (type=create, resource=task, status=NOT_STARTED)
	@Test
	void createTask_onSubscribedBoard_receivesWebSocketOutboxEvent() throws Exception {
		// --- Given: authenticated user and board ---
		String token = loginAndGetToken(SEED_USERNAME, SEED_PASSWORD);
		UUID boardId = findBoardIdByName(token, SEED_BOARD_NAME);

		// --- Given: WebSocket subscription to the board (messages collected in queue) ---
		BlockingQueue<String> wsMessages = new LinkedBlockingQueue<>();

        connectWebSocketToBoard(token, boardId, wsMessages);

        // --- Given: Debezium CDC connector reading outbox table; brief warmup for Kafka consumer ---
		registerDebeziumConnector();
		Thread.sleep(DEBEZIUM_WARMUP_MS);

		// --- When: user creates a task on the board via REST ---
		String taskName = "New task from WebSocket test";
		createTaskOnBoard(token, boardId, taskName);

		// --- Then: expect a WebSocket message for the task create event (outbox → Debezium → Kafka → app) ---
		String matchingMessage = awaitWebSocketMessage(
				wsMessages,
				OUTBOX_MESSAGE_TIMEOUT_SEC,
				msg -> msg.contains("type=create")
						&& msg.contains("resource=task")
						&& msg.contains("key=status")
						&& msg.contains("value=NOT_STARTED")
		);

		assertThat(matchingMessage)
				.as("WebSocket should receive task create outbox event within %s seconds", OUTBOX_MESSAGE_TIMEOUT_SEC)
				.isNotNull();
	}

	/** Logs in via POST /api/login and returns the JWT for subsequent authenticated requests. */
	private String loginAndGetToken(String username, String password) {
		LoginRequest request = LoginRequest.builder().username(username).password(password).build();
		ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/api/login", request, LoginResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		return Objects.requireNonNull(response.getBody()).getToken();
	}

	/** Fetches GET /api/boards, finds the board by name, and returns its ID. */
	private UUID findBoardIdByName(String token, String boardName) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		ResponseEntity<String> response = restTemplate.exchange(
				"/api/boards",
				HttpMethod.GET,
				new HttpEntity<>(headers),
				String.class
		);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();

		ObjectMapper mapper = new ObjectMapper();
		JsonNode content = mapper.readTree(response.getBody()).get("content");
		assertThat(content).isNotNull();
		assertThat(content.isArray()).isTrue();
		assertThat(content.size()).isGreaterThan(0);

		for (int i = 0; i < content.size(); i++) {
			JsonNode board = content.get(i);
			if (boardName.equals(board.path("name").asText())) {
				return UUID.fromString(board.get("id").asText());
			}
		}
		throw new AssertionError("Board with name \"" + boardName + "\" not found");
	}

	/** Opens a WebSocket to /ws/board/{boardId} with protocol "board-v1" and token; enqueues all received text messages. */
	private WebSocketSession connectWebSocketToBoard(String token, UUID boardId, BlockingQueue<String> messageSink) throws Exception {
		WebSocketClient client = new StandardWebSocketClient();
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.setSecWebSocketProtocol(List.of("board-v1", token));

		URI rootUri = URI.create(restTemplate.getRootUri());
		String wsUrl = String.format("ws://%s:%d/ws/board/%s", rootUri.getHost(), rootUri.getPort(), boardId);

		return client.execute(
                new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                        messageSink.offer(message.getPayload());
                    }
                },
                headers,
                URI.create(wsUrl)
        ).get(WEBSOCKET_HANDSHAKE_TIMEOUT_SEC, TimeUnit.SECONDS);
	}

	/** Registers the Debezium connector to stream the public.outbox table into Kafka. */
	private void registerDebeziumConnector() throws Exception {
		ConnectorConfiguration connector = ConnectorConfiguration
				.forJdbcContainer(postgresContainer)
				.with("topic.prefix", "debezium")
				.with("slot.name", "debezium_1")
				.with("schema.include.list", "public")
				// otherwise, debezium could not access file "decoderbufs"
				// https://stackoverflow.com/questions/59978213/debezium-could-not-access-file-decoderbufs-using-postgres-11-with-default-plug
				.with("plugin.name", "pgoutput")
				.with("table.include.list", "public.outbox");
		debeziumContainer.registerConnector("to-do-outbox", connector);
	}

	/** Creates a task via POST /api/boards/{boardId}/tasks and asserts 201 and response body. */
	private void createTaskOnBoard(String token, UUID boardId, String taskName) {
		TaskCreateRequest request = TaskCreateRequest.builder()
				.name(taskName)
				.description("abc")
				.build();
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		ResponseEntity<TaskResponse> response = restTemplate.exchange(
				"/api/boards/{boardId}/tasks",
				HttpMethod.POST,
				new HttpEntity<>(request, headers),
				TaskResponse.class,
				boardId
		);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		TaskResponse body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getName()).isEqualTo(taskName);
	}

	/** Polls the queue until a message matches the predicate or the timeout (seconds) elapses; returns the match or null. */
	private String awaitWebSocketMessage(BlockingQueue<String> messages, int timeoutSec, java.util.function.Predicate<String> matcher) throws InterruptedException {
		Instant deadline = Instant.now().plusSeconds(timeoutSec);
		while (Instant.now().isBefore(deadline)) {
			long remainingMs = Duration.between(Instant.now(), deadline).toMillis();
			String msg = messages.poll(Math.max(1, remainingMs), TimeUnit.MILLISECONDS);
			if (msg != null && matcher.test(msg)) {
				return msg;
			}
		}
		return null;
	}
}
