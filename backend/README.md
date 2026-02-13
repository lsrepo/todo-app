# TODO List API

Board-centric TODO API: boards and tasks with plural REST endpoints, command pattern for writes, and a transactional outbox table.

## Prerequisites

- Java 21
- Docker

## Running the application

From the `backend` directory:

1. Start infrastructure (PostgreSQL, Kafka broker, Kafka Connect with Debezium, Kafka UI):

   ```bash
   docker compose up -d
   ```

2. Run the Spring Boot app (creates the schema and runs the seeder; the Debezium connector needs the `outbox` table to exist):

   ```bash
   ./gradlew bootRun
   ```

3. Register the Debezium PostgreSQL connector (run this *after* the app has started, since the Boot app creates the tables and seeding):

   ```bash
   ./scripts/setup-debezium.sh
   ```

## Outbox event dataflow

- **HTTP request**: Client calls a REST endpoint (for example, create/update/delete board or task).
- **Command handler**: The controller turns the request into a command; the command handler persists the aggregate and, via `OutboxSupport`, creates an `outbox` row in PostgreSQL in the same transaction.
- **PostgreSQL**: The `outbox` table holds the event payload as JSON.
- **Debezium / Kafka Connect**: Debezium monitors the `outbox` table and publishes changes to the Kafka topic `debezium.public.outbox`.
- **Kafka topic**: Each outbox row becomes a Kafka message (with `schema` + `payload` or plain JSON, depending on connector config).
- **Spring consumer**: `OutboxKafkaConsumer` subscribes to `debezium.public.outbox`, extracts each outbox row (including `board_id`), and broadcasts a concise message over WebSocket to any clients listening for that board.

## Testing

**Strategy:** Tests live in two source sets. **`src/test`** holds unit and slice tests (suffix `Test`): plain unit tests with mocks (command handlers, services, auth), and controller slice tests with `@WebMvcTest` (HTTP behaviour, validation, error responses). **`src/integrationTest`** holds full-context tests (suffix `IT`): `@SpringBootTest` with Testcontainers (PostgreSQL, optionally Kafka) to exercise real wiring and persistence. Slice tests stay in `src/test` so they run with the fast unit suite; only tests that need the full application context go in `src/integrationTest`.

### Running the tests

- **Unit tests** (`./gradlew test`): Command handlers, services, auth, controllers (including validation). Mocks or `@WebMvcTest`; no real DB. Fast.

- **Integration tests** (`./gradlew integrationTest`): Full app with Testcontainers (Docker required). End-to-end flows and persistence. Slower.

## API

The API is documented in Swagger when the app is running: [Swagger UI](http://localhost:8088/swagger-ui.html), [OpenAPI JSON](http://localhost:8088/v3/api-docs). You can import the OpenAPI JSON into Postman to get a collection.

### Swagger UI: login and attaching the access token

Protected endpoints require a JWT in the `Authorization` header. In Swagger UI:

1. **Login**: Open the **Auth** section, expand **POST /api/login**, click **Try it out**, enter a username and password (e.g. `user1` / `password` from the seeder), then **Execute**. Copy the `token` from the response body.
2. **Authorize**: Click the **Authorize** button (lock icon) at the top of the page. Paste the token into the **bearer-jwt** field (paste only the token, not `Bearer `). Click **Authorize**, then **Close**.
3. **Call protected APIs**: All subsequent requests (e.g. GET /api/boards, GET /api/boards/{id}) will automatically include `Authorization: Bearer <token>` in the request headers.

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


## Design

- **Commands**: Create/Update/Delete operations are implemented as commands; handlers persist the entity and an outbox row in the same transaction.
- **Outbox**: Events are stored in the `outbox` table (no processor or WebSocket in this version).
- **Mapping**: Request DTO → Command via command factories in the web layer (`BoardCommandFactory`, `TaskCommandFactory`); entity → response DTO via `BoardMapper` / `TaskMapper`.
