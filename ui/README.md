# Todo Board UI (Testing)

This is a small React UI for testing the todo board backend.

## Features

- Route `/board/:id` that shows a 3-column board (Not started, In progress, Done).
- Header with:
  - **User impersonation** dropdown (`user1`–`user5`, password always `password`), which calls `POST /api/login` and stores the JWT.
  - **Boards** dropdown populated from `GET /api/boards`, used to navigate between boards.
- Tasks are loaded from `GET /api/boards/{boardId}/tasks` and grouped by status.
- Clicking a task lets you **inline edit** the name; pressing Enter or blurring saves via `PUT /api/boards/{boardId}/tasks/{taskId}`.
- Each column has an input to **create tasks** via `POST /api/boards/{boardId}/tasks`.
- Realtime updates via **WebSocket**:
  - Connects to `ws://localhost:8088/ws/board/{boardId}`.
  - Sends subprotocols `["board-v1", token]`, and the backend reads the JWT from `Sec-WebSocket-Protocol`.
  - Parses messages like `type=edit;resource=task;id=...;key=status;value=IN_PROGRESS` and updates local state.

## Running the UI

1. Start the backend on port `8088`.
2. From this `ui` directory:

```bash
npm install
npm run dev
```

3. Open the printed Vite dev URL (usually `http://localhost:5173`).
4. Pick a user and board in the header; the board at `/board/:id` will load and stay in sync with the backend.

