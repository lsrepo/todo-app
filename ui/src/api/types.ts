export type TaskStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";

export interface Board {
  id: string;
  name: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Task {
  id: string;
  boardId: string;
  name: string;
  description?: string | null;
  dueDate?: string | null;
  status: TaskStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface UserOption {
  username: string;
  password: string;
}

