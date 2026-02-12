import { useState, KeyboardEvent } from "react";
import type { Task, TaskStatus } from "../api/types";
import { TaskCard } from "./TaskCard";

interface TaskColumnProps {
  title: string;
  status: TaskStatus;
  tasks: Task[];
  onCreateTask: (name: string, status: TaskStatus) => Promise<void>;
  onUpdateTaskName: (taskId: string, name: string) => Promise<void>;
}

export function TaskColumn({
  title,
  status,
  tasks,
  onCreateTask,
  onUpdateTaskName
}: TaskColumnProps) {
  const [draft, setDraft] = useState("");
  const [creating, setCreating] = useState(false);

  const handleCreate = async () => {
    const trimmed = draft.trim();
    if (!trimmed || creating) return;
    try {
      setCreating(true);
      await onCreateTask(trimmed, status);
      setDraft("");
    } finally {
      setCreating(false);
    }
  };

  const handleKeyDown = async (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      await handleCreate();
    }
  };

  return (
    <div className="column">
      <div className="column-header">
        <div className="column-title">{title}</div>
        <div className="column-count">{tasks.length}</div>
      </div>
      <div className="tasks-list">
        {tasks.map((task) => (
          <TaskCard
            key={task.id}
            task={task}
            onSaveName={(name) => onUpdateTaskName(task.id, name)}
          />
        ))}
      </div>
      <div className="add-task-row">
        <input
          className="add-task-input"
          placeholder="Add task and press Enter"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={creating}
        />
      </div>
    </div>
  );
}

