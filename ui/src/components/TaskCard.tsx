import { useState, KeyboardEvent, FocusEvent } from "react";
import { useDraggable } from "@dnd-kit/core";
import type { Task } from "../api/types";

/** Format ISO date string to DD/MM/YYYY for display */
function formatDueDate(iso: string | null | undefined): string {
  if (!iso) return "";
  try {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return "";
    const day = String(d.getUTCDate()).padStart(2, "0");
    const month = String(d.getUTCMonth() + 1).padStart(2, "0");
    const year = d.getUTCFullYear();
    return `${day}/${month}/${year}`;
  } catch {
    return "";
  }
}

/** Parse DD/MM/YYYY (or D/M/YYYY) to ISO string at UTC midnight, or null if invalid/empty */
function parseDueDateInput(text: string): string | null {
  const trimmed = text.trim();
  if (!trimmed) return null;
  const parts = trimmed.split("/").map((p) => p.trim());
  if (parts.length !== 3) return null;
  const day = parseInt(parts[0], 10);
  const month = parseInt(parts[1], 10) - 1;
  const year = parseInt(parts[2], 10);
  if (Number.isNaN(day) || Number.isNaN(month) || Number.isNaN(year)) return null;
  if (month < 0 || month > 11) return null;
  const date = new Date(Date.UTC(year, month, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month || date.getUTCDate() !== day) return null;
  return date.toISOString();
}

function TrashIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M3 6h18" />
      <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
      <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
      <line x1="10" y1="11" x2="10" y2="17" />
      <line x1="14" y1="11" x2="14" y2="17" />
    </svg>
  );
}

interface TaskCardProps {
  task: Task;
  onSaveName: (name: string) => Promise<void>;
  onSaveDueDate: (dueDate: string | null) => Promise<void>;
  onDelete: () => Promise<void>;
}

export function TaskCard({ task, onSaveName, onSaveDueDate, onDelete }: TaskCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [value, setValue] = useState(task.name);
  const [saving, setSaving] = useState(false);
  const [isEditingDueDate, setIsEditingDueDate] = useState(false);
  const [dueDateInput, setDueDateInput] = useState(() => formatDueDate(task.dueDate));
  const [savingDueDate, setSavingDueDate] = useState(false);

  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: task.id,
    data: { type: "task", task }
  });

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

  const handleDeleteClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    void onDelete();
  };

  const commitDueDate = async () => {
    if (savingDueDate) return;
    const parsed = parseDueDateInput(dueDateInput);
    const currentIso = task.dueDate ?? null;
    // Skip API call only when both are null or same calendar day
    const sameDay = (a: string, b: string) =>
      new Date(a).toISOString().slice(0, 10) === new Date(b).toISOString().slice(0, 10);
    if (parsed === null && !currentIso) {
      exitDueDateEdit();
      return;
    }
    if (parsed !== null && currentIso !== null && sameDay(parsed, currentIso)) {
      exitDueDateEdit();
      return;
    }
    try {
      setSavingDueDate(true);
      await onSaveDueDate(parsed);
      setDueDateInput(formatDueDate(parsed ?? undefined));
      setIsEditingDueDate(false);
    } finally {
      setSavingDueDate(false);
    }
  };

  const handleDueDateKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      e.stopPropagation();
      void commitDueDate();
    } else if (e.key === "Escape") {
      e.stopPropagation();
      exitDueDateEdit();
    }
  };

  const exitDueDateEdit = () => {
    setIsEditingDueDate(false);
    setDueDateInput(formatDueDate(task.dueDate));
  };

  const handleDueDateRowClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!isEditingDueDate) {
      setIsEditingDueDate(true);
      setDueDateInput(formatDueDate(task.dueDate));
    }
  };

  const dueDateDisplay = formatDueDate(task.dueDate);
  const dueDateButtonLabel = dueDateDisplay ? dueDateDisplay : "dd/mm/yyyy";

  return (
    <div
      ref={setNodeRef}
      className={`task-card${isDragging ? " task-card--dragging" : ""}`}
      onClick={() => setIsEditing(true)}
    >
      <div className="task-card-body" {...listeners} {...attributes}>
        <div className="task-card-content">{task.name}</div>
        <div className="task-card-due-row" onClick={handleDueDateRowClick}>
          {isEditingDueDate ? (
            <input
              className="task-due-input"
              placeholder="dd/mm/yyyy"
              value={dueDateInput}
              onChange={(e) => setDueDateInput(e.target.value)}
              onKeyDown={handleDueDateKeyDown}
              onBlur={() => void commitDueDate()}
              disabled={savingDueDate}
              onClick={(e) => e.stopPropagation()}
            />
          ) : (
            <button
              type="button"
              className="task-due-btn"
              title={dueDateDisplay ? `Due date: ${dueDateDisplay}` : "Edit due date"}
              aria-label={dueDateButtonLabel}
            >
              {dueDateButtonLabel}
            </button>
          )}
        </div>
      </div>
      <button
        type="button"
        className="task-delete-btn"
        onClick={handleDeleteClick}
        title="Remove task"
        aria-label="Remove task"
      >
        <TrashIcon />
      </button>
    </div>
  );
}

