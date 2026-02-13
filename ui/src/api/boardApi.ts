import { apiFetch } from "./client";
import type { Board, Page, Task, TaskStatus } from "./types";

export interface LoginResponse {
  token: string;
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  return apiFetch<LoginResponse>("/login", {
    method: "POST",
    body: JSON.stringify({ username, password })
  });
}

export async function getBoards(token: string): Promise<Page<Board>> {
  return apiFetch<Page<Board>>("/boards", {}, token);
}

export async function getBoard(boardId: string, token: string): Promise<Board> {
  return apiFetch<Board>(`/boards/${boardId}`, {}, token);
}

export async function getTasks(
  boardId: string,
  token: string,
  params?: { size?: number }
): Promise<Page<Task>> {
  const search = new URLSearchParams();
  if (params?.size != null) {
    search.set("size", String(params.size));
  }

  const query = search.toString();
  const path = query ? `/boards/${boardId}/tasks?${query}` : `/boards/${boardId}/tasks`;

  return apiFetch<Page<Task>>(path, {}, token);
}

export async function createTask(
  boardId: string,
  token: string,
  payload: { name: string; status: TaskStatus }
): Promise<Task> {
  return apiFetch<Task>(
    `/boards/${boardId}/tasks`,
    {
      method: "POST",
      body: JSON.stringify(payload)
    },
    token
  );
}

export async function updateTask(
  boardId: string,
  taskId: string,
  token: string,
  payload: Partial<Pick<Task, "name" | "status" | "description" | "dueDate">>
): Promise<Task> {
  return apiFetch<Task>(
    `/boards/${boardId}/tasks/${taskId}`,
    {
      method: "PUT",
      body: JSON.stringify(payload)
    },
    token
  );
}

export async function deleteTask(
  boardId: string,
  taskId: string,
  token: string
): Promise<void> {
  return apiFetch<void>(
    `/boards/${boardId}/tasks/${taskId}`,
    { method: "DELETE" },
    token
  );
}

