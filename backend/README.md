# TODO List API

Board-centric TODO API: boards and tasks with plural REST endpoints, command pattern for writes, and a transactional outbox table.

## Prerequisites

- Java 25
- Docker (optional, for running PostgreSQL locally)

## Running the application

### With local PostgreSQL

1. Start PostgreSQL (e.g. Docker: `docker run -d --name todo-db -e POSTGRES_DB=todo -e POSTGRES_USER=todo -e POSTGRES_PASSWORD=todo -p 5432:5432 postgres:16-alpine`)
2. Run the app: `./gradlew bootRun`

### With Docker Compose (PostgreSQL, Kafka, Debezium, Kafka UI)

From the `backend` directory:

1. Start infrastructure (PostgreSQL, Kafka broker, Kafka Connect with Debezium, Kafka UI):

   ```bash
   docker compose up -d
   ```

2. Run the Spring Boot app:

   ```bash
   ./gradlew bootRun
   ```

3. Trigger outbox events using the existing REST API (for example, create/update/delete boards or tasks).
4. The Debezium outbox connector writes to the Kafka topic `debezium.public.outbox`.
5. The application consumes this topic and logs the outbox payload for each message. Look for log lines like:

   ```
   Outbox payload: {...}
   ```

   in the application console output.

## Outbox event dataflow

- **HTTP request**: Client calls a REST endpoint (for example, create/update/delete board or task).
- **Command handler**: The controller turns the request into a command; the command handler persists the aggregate and, via `OutboxSupport`, creates an `outbox` row in PostgreSQL in the same transaction.
- **PostgreSQL**: The `outbox` table holds the event payload as JSON.
- **Debezium / Kafka Connect**: Debezium monitors the `outbox` table and publishes changes to the Kafka topic `debezium.public.outbox`.
- **Kafka topic**: Each outbox row becomes a Kafka message (with `schema` + `payload` or plain JSON, depending on connector config).
- **Spring consumer**: `OutboxKafkaConsumer` subscribes to `debezium.public.outbox`, extracts each outbox row (including `board_id`), and broadcasts a concise message over WebSocket to any clients listening for that board.

### Default configuration

The app expects PostgreSQL at `localhost:5432` with database `todo`, user `todo`, password `todo`. Adjust `src/main/resources/application.yaml` if needed.

## Running tests


### Run only unit tests

```bash
./gradlew test
```

### Run only integration tests

```bash
./gradlew integrationTest
```

Tests use Testcontainers to start a PostgreSQL container (requires Docker).

### What each type of test focuses on

- **Unit tests (`src/test/java`)**
  - Focus on **in-process business logic**: command handlers, services, domain behavior.
  - Use mocks for repositories/mappers where needed.
  - Fast, no Spring context or real database required.

- **Integration tests (`src/integrationTest/java`)**
  - Focus on **application wiring and external contracts**:
    - HTTP endpoints, request/response mapping, validation, and error formats (e.g. `TaskControllerValidationTest`).
    - Interaction with infrastructure like the database via Spring/JPA (when using `@SpringBootTest` + Testcontainers).
  - Slower than unit tests, but give confidence that the app behaves correctly from the outside.

## API

- **Base path**: `/api`
- **Boards**: `GET /api/boards`, `POST /api/boards`, `GET /api/boards/{boardId}`, `PUT /api/boards/{boardId}`, `DELETE /api/boards/{boardId}`
- **Tasks** (scoped by board): `GET /api/boards/{boardId}/tasks`, `POST /api/boards/{boardId}/tasks`, `GET /api/boards/{boardId}/tasks/{taskId}`, `PUT /api/boards/{boardId}/tasks/{taskId}`, `DELETE /api/boards/{boardId}/tasks/{taskId}`

Task list supports query params: `status` (NOT_STARTED, IN_PROGRESS, COMPLETED), `dueFrom`, `dueTo`, and Spring `Pageable` (`sort`, `page`, `size`).

## Documentation

- **OpenAPI (Swagger)**: [http://localhost:8088/swagger-ui.html](http://localhost:8088/swagger-ui.html) when the app is running
- **OpenAPI JSON**: [http://localhost:8088/v3/api-docs](http://localhost:8088/v3/api-docs)

You can import the OpenAPI JSON into Postman to get a collection.

## WebSocket board stream

- **Endpoint**: `ws://localhost:8088/ws/board/{boardId}`
  - Replace `{boardId}` with the UUID of the board you want to listen to.
  - Connecting to this URL subscribes the client to changes for that board.
- **What you receive**: For any board or task change on that board, the client receives a single-line text message derived from the outbox event with the format:

  ```text
  type=<create|edit|delete>;resource=<board|task>;id=<uuid>;key=<field>;value=<value>
  ```

  - `type`: derived from the event type (e.g. `BoardCreated` → `create`, `TaskDeleted` → `delete`, others → `edit`).
  - `resource`: `board` or `task` based on the aggregate type.
  - `id`: the aggregate ID (board or task).
  - `key`: typically the main field of interest (for tasks, `status` when present; otherwise `name`; for boards, `name`).
  - `value`: the new value for that field, with `;` and `=` escaped as `\;` and `\=`.

- **Example client (vanilla JS)**:

  ```js
  import Stomp from 'stompjs';

  const boardId = '00000000-0000-0000-0000-000000000000';
  const socket = new WebSocket('ws://localhost:8088/ws');
  const stompClient = Stomp.over(socket);

  stompClient.connect({}, () => {
    console.log('Connected to STOMP board stream');

    stompClient.subscribe(`/topic/boards/${boardId}`, (message) => {
      console.log('Board stream message:', message.body);
      // Example payload:
      // type=create;resource=task;id=...;key=status;value=NOT_STARTED
    });
  });
  ```

## Design

- **Commands**: Create/Update/Delete operations are implemented as commands; handlers persist the entity and an outbox row in the same transaction.
- **Outbox**: Events are stored in the `outbox` table (no processor or WebSocket in this version).
- **Mapping**: Request DTO → Command via static factory on the command; entity → response DTO via `BoardMapper` / `TaskMapper`.
