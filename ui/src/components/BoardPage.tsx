import { useEffect, useMemo, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent
} from "@dnd-kit/core";
import type { Board, Task, TaskStatus } from "../api/types";
import { getBoard, getTasks, createTask, updateTask, deleteTask } from "../api/boardApi";
import { useBoardWebSocket, OutboxMessage } from "../hooks/useBoardWebSocket";
import { TaskColumn } from "./TaskColumn";

const VALID_STATUSES: TaskStatus[] = ["NOT_STARTED", "IN_PROGRESS", "COMPLETED"];

function isValidStatus(id: unknown): id is TaskStatus {
  return typeof id === "string" && VALID_STATUSES.includes(id as TaskStatus);
}

interface BoardPageProps {
  token: string | null;
  onWebSocketStatusChange?: (connected: boolean) => void;
}

export function BoardPage({ token, onWebSocketStatusChange }: BoardPageProps) {
  const { boardId } = useParams<{ boardId: string }>();
  const [board, setBoard] = useState<Board | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTask, setActiveTask] = useState<Task | null>(null);

  const loadData = useCallback(async () => {
    if (!boardId || !token) {
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const [boardRes, tasksPage] = await Promise.all([
        getBoard(boardId, token),
        getTasks(boardId, token, { size: 1000 })
      ]);
      setBoard(boardRes);
      setTasks(tasksPage.content);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, [boardId, token]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const handleWsMessage = useCallback(
    (msg: OutboxMessage) => {
      if (msg.resource === "task") {
        setTasks((prev) => {
          if (msg.type === "delete") {
            return prev.filter((t) => t.id !== msg.id);
          }
          if (msg.type === "edit") {
            return prev.map((t) => {
              if (t.id !== msg.id) return t;
              if (msg.key === "status") {
                return { ...t, status: msg.value as TaskStatus };
              }
              if (msg.key === "name") {
                return { ...t, name: msg.value };
              }
              if (msg.key === "dueDate") {
                return { ...t, dueDate: msg.value || null };
              }
              if (msg.key === "description") {
                return { ...t, description: msg.value };
              }
              return t;
            });
          }
          if (msg.type === "create") {
            if (prev.some((t) => t.id === msg.id)) return prev;
            if (!boardId) return prev;
            const now = new Date().toISOString();
            const newTask: Task = {
              id: msg.id,
              boardId,
              name: msg.key === "name" ? msg.value : "New task",
              status: (msg.key === "status" ? msg.value : "NOT_STARTED") as TaskStatus,
              createdAt: now,
              updatedAt: now
            };
            return [...prev, newTask];
          }
          return prev;
        });
      } else if (msg.resource === "board" && msg.type === "edit" && msg.key === "name") {
        setBoard((prev) => (prev ? { ...prev, name: msg.value } : prev));
      }
    },
    [boardId]
  );

  const { connected } = useBoardWebSocket(boardId, token, handleWsMessage);

  useEffect(() => {
    if (onWebSocketStatusChange) {
      onWebSocketStatusChange(connected);
    }
  }, [connected, onWebSocketStatusChange]);

  const grouped = useMemo(() => {
    const byStatus: Record<TaskStatus, Task[]> = {
      NOT_STARTED: [],
      IN_PROGRESS: [],
      COMPLETED: []
    };
    for (const task of tasks) {
      byStatus[task.status].push(task);
    }
    return byStatus;
  }, [tasks]);

  const handleCreateTask = async (name: string, status: TaskStatus) => {
    if (!boardId || !token) return;
    const created = await createTask(boardId, token, { name, status });
    setTasks((prev) => {
      const exists = prev.some((t) => t.id === created.id);
      return exists ? prev : [...prev, created];
    });
  };

  const handleUpdateTaskName = async (taskId: string, name: string) => {
    if (!boardId || !token) return;
    const updated = await updateTask(boardId, taskId, token, { name });
    setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
  };

  const handleUpdateTaskDueDate = async (taskId: string, dueDate: string | null) => {
    if (!boardId || !token) return;
    const updated = await updateTask(boardId, taskId, token, { dueDate });
    setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
  };

  const handleDeleteTask = async (taskId: string) => {
    if (!boardId || !token) return;
    await deleteTask(boardId, taskId, token);
    setTasks((prev) => prev.filter((t) => t.id !== taskId));
  };

  const handleUpdateTaskStatus = useCallback(
    async (taskId: string, newStatus: TaskStatus) => {
      if (!boardId || !token) return;
      const task = tasks.find((t) => t.id === taskId);
      if (!task || task.status === newStatus) return;
      const previousTasks = tasks;
      setTasks((prev) =>
        prev.map((t) => (t.id === taskId ? { ...t, status: newStatus } : t))
      );
      try {
        await updateTask(boardId, taskId, token, { status: newStatus });
      } catch {
        setTasks(previousTasks);
      }
    },
    [boardId, token, tasks]
  );

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 8 }
    })
  );

  const onDragStart = useCallback(
    (event: DragStartEvent) => {
      const task =
        (event.active.data.current?.task as Task | undefined) ??
        tasks.find((t) => t.id === event.active.id) ??
        null;
      setActiveTask(task);
    },
    [tasks]
  );

  const onDragEnd = useCallback(
    (event: DragEndEvent) => {
      setActiveTask(null);
      const { active, over } = event;
      if (!over || !isValidStatus(over.id)) return;
      const taskId = String(active.id);
      handleUpdateTaskStatus(taskId, over.id);
    },
    [handleUpdateTaskStatus]
  );

  if (!boardId) {
    return (
      <div className="board-container">
        <div className="board-empty-message">
          Select a board from the header to get started.
        </div>
      </div>
    );
  }

  return (
    <div className="board-container">
      <div className="board-title-row">
        <div>
          <div className="board-title">
            {board ? board.name : "Loading board..."}
          </div>
          {board?.description && (
            <div className="board-subtitle">{board.description}</div>
          )}
        </div>
        <div className="board-subtitle">
          {loading
            ? "Loading..."
            : error
            ? `Error: ${error}`
            : `Tasks: ${tasks.length} | WebSocket: ${
                connected ? "connected" : "disconnected"
              }`}
        </div>
      </div>

      <DndContext
        sensors={sensors}
        onDragStart={onDragStart}
        onDragEnd={onDragEnd}
      >
        <div className="columns">
          <TaskColumn
            title="Not started"
            status="NOT_STARTED"
            tasks={grouped.NOT_STARTED}
            onCreateTask={handleCreateTask}
            onUpdateTaskName={handleUpdateTaskName}
            onUpdateTaskDueDate={handleUpdateTaskDueDate}
            onDeleteTask={handleDeleteTask}
          />
          <TaskColumn
            title="In progress"
            status="IN_PROGRESS"
            tasks={grouped.IN_PROGRESS}
            onCreateTask={handleCreateTask}
            onUpdateTaskName={handleUpdateTaskName}
            onUpdateTaskDueDate={handleUpdateTaskDueDate}
            onDeleteTask={handleDeleteTask}
          />
          <TaskColumn
            title="Done"
            status="COMPLETED"
            tasks={grouped.COMPLETED}
            onCreateTask={handleCreateTask}
            onUpdateTaskName={handleUpdateTaskName}
            onUpdateTaskDueDate={handleUpdateTaskDueDate}
            onDeleteTask={handleDeleteTask}
          />
        </div>
        <DragOverlay>
          {activeTask ? (
            <div className="task-card task-card--overlay">
              <div className="task-card-body">
                <div className="task-card-content">{activeTask.name}</div>
              </div>
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>
    </div>
  );
}

