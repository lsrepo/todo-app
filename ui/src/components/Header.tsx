import type { Board, UserOption } from "../api/types";

interface HeaderProps {
  users: UserOption[];
  currentUser: string;
  boards: Board[];
  selectedBoardId: string | null;
  loginError: string | null;
  onUserChange: (username: string) => void;
  onBoardChange: (boardId: string) => void;
  wsConnected: boolean;
}

export function Header({
  users,
  currentUser,
  boards,
  selectedBoardId,
  loginError,
  onUserChange,
  onBoardChange,
  wsConnected
}: HeaderProps) {
  return (
    <header className="app-header">
      <div className="header-section">
        <span className="header-label">Impersonate user</span>
        <select
          className="header-select"
          value={currentUser}
          onChange={(e) => onUserChange(e.target.value)}
        >
          {users.map((u) => (
            <option key={u.username} value={u.username}>
              {u.username}
            </option>
          ))}
        </select>
        {loginError && <span className="header-error">{loginError}</span>}
      </div>

      <div className="header-section">
        <span className="header-label">Board</span>
        <select
          className="header-select"
          value={selectedBoardId ?? ""}
          onChange={(e) => onBoardChange(e.target.value)}
        >
          <option value="">Select board</option>
          {boards.map((b) => (
            <option key={b.id} value={b.id}>
              {b.name}
            </option>
          ))}
        </select>
      </div>

      <div className="header-section">
        <span className="header-label">Realtime</span>
        <span className="ws-status">
          {wsConnected ? "Connected" : "Disconnected"}
        </span>
      </div>
    </header>
  );
}

