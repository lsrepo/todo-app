import { useEffect, useMemo, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import type { Board, Task, TaskStatus } from "../api/types";
import { getBoard, getTasks, createTask, updateTask } from "../api/boardApi";
import { useBoardWebSocket, OutboxMessage } from "../hooks/useBoardWebSocket";
import { TaskColumn } from "./TaskColumn";

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
              return t;
            });
          }
          // For create, keep it simple and just reload tasks.
          if (msg.type === "create") {
            void loadData();
          }
          return prev;
        });
      } else if (msg.resource === "board" && msg.type === "edit" && msg.key === "name") {
        setBoard((prev) => (prev ? { ...prev, name: msg.value } : prev));
      }
    },
    [loadData]
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

      <div className="columns">
        <TaskColumn
          title="Not started"
          status="NOT_STARTED"
          tasks={grouped.NOT_STARTED}
          onCreateTask={handleCreateTask}
          onUpdateTaskName={handleUpdateTaskName}
        />
        <TaskColumn
          title="In progress"
          status="IN_PROGRESS"
          tasks={grouped.IN_PROGRESS}
          onCreateTask={handleCreateTask}
          onUpdateTaskName={handleUpdateTaskName}
        />
        <TaskColumn
          title="Done"
          status="COMPLETED"
          tasks={grouped.COMPLETED}
          onCreateTask={handleCreateTask}
          onUpdateTaskName={handleUpdateTaskName}
        />
      </div>
    </div>
  );
}

