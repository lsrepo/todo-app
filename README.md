# TODO Application — Product Overview

A board-centric TODO application for teams: users manage tasks on shared boards with role-based access and see changes in real time. A web UI is available for testing the application; it is still under development.

---

## Features

**Boards and tasks**  
Work is organised in boards. Each task has a name, optional description, optional due date, and status (Not started, In progress, Completed). Users can filter tasks (e.g. by status or due-date range) and sort them (e.g. by due date). Full create, read, update, and delete for both boards and tasks.

**Authentication**  
Users log in with username and password. Access to the application and to each board is controlled by permissions.

**Shared boards and roles**  
Boards can be shared with other users. Two roles: **Owner** (full control, including delete and sharing) and **Editor** (can view and edit tasks). Only users with access can open a board or see its tasks. The board creator is the owner by default.

**Real-time updates**  
When someone creates, updates, or deletes a board or task, everyone who has that board open sees the change immediately—no refresh needed. Updates are pushed to connected clients as they happen.

**Deployment**  
The application runs in Docker; the backend and supporting services (database, event pipeline) can be started with Docker Compose. Run and API details are in the backend and UI documentation.

---

## Architecture (high level)

- **UI** — Web interface where users log in, manage boards and tasks, and receive live updates.
- **Application server** — Handles requests, stores data, and pushes events to connected clients.
- **Database** — Stores boards, tasks, users, and permissions.
- **Event pipeline** — Captures changes from the database and delivers them to the server, which then broadcasts to users viewing the relevant board.

```
                    +----------+
                    |    UI    |
                    +----+-----+
                         |
              HTTP       |       WebSocket
              (REST)     |       (bidirectional)
                         v
                    +----------+
                    |  Server  |<--------+ consume
                    +----+-----+         |
                         |               |
                    DB   |               |
                         v               |
                    +----------+   +-----+----+
                    | Database  |   |  Events  |
                    +----+-----+   +---------+
                         |              ^
                         |   CDC        |
                         +--------------+
```

The UI talks to the server over HTTP (for loading and saving data and for login) and over a bidirectional WebSocket (for live updates). The server writes to the database; a change-data capture (CDC) process streams those changes into an event bus; the server consumes events and pushes them to the right WebSocket clients so everyone viewing a board stays in sync.

---

For run instructions, API reference, and technical details, see the backend and UI documentation in this repository.
