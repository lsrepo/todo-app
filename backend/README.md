# TODO List API

Board-centric TODO API: boards and tasks with plural REST endpoints, command pattern for writes, and a transactional outbox table.

## Prerequisites

- Java 25
- Docker (optional, for running PostgreSQL locally)

## Running the application

### With local PostgreSQL

1. Start PostgreSQL (e.g. Docker: `docker run -d --name todo-db -e POSTGRES_DB=todo -e POSTGRES_USER=todo -e POSTGRES_PASSWORD=todo -p 5432:5432 postgres:16-alpine`)
2. Run the app: `./gradlew bootRun`

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

- **OpenAPI (Swagger)**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) when the app is running
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

You can import the OpenAPI JSON into Postman to get a collection.

## Design

- **Commands**: Create/Update/Delete operations are implemented as commands; handlers persist the entity and an outbox row in the same transaction.
- **Outbox**: Events are stored in the `outbox` table (no processor or WebSocket in this version).
- **Mapping**: Request DTO → Command via static factory on the command; entity → response DTO via `BoardMapper` / `TaskMapper`.
