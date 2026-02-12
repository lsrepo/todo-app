import { useState, KeyboardEvent, FocusEvent } from "react";
import type { Task } from "../api/types";

interface TaskCardProps {
  task: Task;
  onSaveName: (name: string) => Promise<void>;
}

export function TaskCard({ task, onSaveName }: TaskCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [value, setValue] = useState(task.name);
  const [saving, setSaving] = useState(false);

  const commit = async () => {
    if (saving) return;
    const trimmed = value.trim();
    if (!trimmed || trimmed === task.name) {
      setIsEditing(false);
      setValue(task.name);
      return;
    }
    try {
      setSaving(true);
      await onSaveName(trimmed);
    } finally {
      setSaving(false);
      setIsEditing(false);
    }
  };

  const handleKeyDown = async (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      await commit();
    } else if (e.key === "Escape") {
      setIsEditing(false);
      setValue(task.name);
    }
  };

  const handleBlur = async (_e: FocusEvent<HTMLInputElement>) => {
    await commit();
  };

  if (isEditing) {
    return (
      <input
        className="task-input"
        autoFocus
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={handleKeyDown}
        onBlur={handleBlur}
        disabled={saving}
      />
    );
  }

  return (
    <div className="task-card" onClick={() => setIsEditing(true)}>
      <div>{task.name}</div>
      <div className="status-pill">{task.status}</div>
    </div>
  );
}

