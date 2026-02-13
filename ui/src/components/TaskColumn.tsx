import { useState, KeyboardEvent } from "react";
import { useDroppable } from "@dnd-kit/core";
import type { Task, TaskStatus } from "../api/types";
import { TaskCard } from "./TaskCard";

interface TaskColumnProps {
  title: string;
  status: TaskStatus;
  tasks: Task[];
  onCreateTask: (name: string, status: TaskStatus) => Promise<void>;
  onUpdateTaskName: (taskId: string, name: string) => Promise<void>;
  onUpdateTaskDueDate: (taskId: string, dueDate: string | null) => Promise<void>;
  onDeleteTask: (taskId: string) => Promise<void>;
}

export function TaskColumn({
  title,
  status,
  tasks,
  onCreateTask,
  onUpdateTaskName,
  onUpdateTaskDueDate,
  onDeleteTask
}: TaskColumnProps) {
  const [draft, setDraft] = useState("");
  const [creating, setCreating] = useState(false);
  const { setNodeRef, isOver, ...attributes } = useDroppable({ id: status });

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
    <div
      ref={setNodeRef}
      className={`column${isOver ? " column--over" : ""}`}
      {...attributes}
    >
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
            onSaveDueDate={(dueDate) => onUpdateTaskDueDate(task.id, dueDate)}
            onDelete={() => onDeleteTask(task.id)}
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

