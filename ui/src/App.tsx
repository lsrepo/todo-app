import { useEffect, useState } from "react";
import { Routes, Route, useLocation, useNavigate, Navigate } from "react-router-dom";
import type { Board, UserOption } from "./api/types";
import { login, getBoards } from "./api/boardApi";
import { Header } from "./components/Header";
import { BoardPage } from "./components/BoardPage";

const USERS: UserOption[] = [
  { username: "user1", password: "password" },
  { username: "user2", password: "password" },
  { username: "user3", password: "password" },
  { username: "user4", password: "password" },
  { username: "user5", password: "password" }
];

function useQuery() {
  const { search } = useLocation();
  return new URLSearchParams(search);
}

export default function App() {
  const navigate = useNavigate();
  const query = useQuery();

  const [currentUser, setCurrentUser] = useState<string>(
    () => window.localStorage.getItem("currentUser") || "user1"
  );
  const [token, setToken] = useState<string | null>(
    () => window.localStorage.getItem("token") || null
  );
  const [loginError, setLoginError] = useState<string | null>(null);
  const [boards, setBoards] = useState<Board[]>([]);
  const [selectedBoardId, setSelectedBoardId] = useState<string | null>(null);
  const [wsConnected, setWsConnected] = useState(false);

  // Auto-login whenever currentUser changes.
  useEffect(() => {
    const selected = USERS.find((u) => u.username === currentUser) ?? USERS[0];

    async function doLogin() {
      try {
        setLoginError(null);
        const res = await login(selected.username, selected.password);
        setToken(res.token);
        window.localStorage.setItem("token", res.token);
        window.localStorage.setItem("currentUser", selected.username);
      } catch (e) {
        setLoginError((e as Error).message);
        setToken(null);
      }
    }

    void doLogin();
  }, [currentUser]);

  // Load boards when we have a token.
  useEffect(() => {
    if (!token) {
      setBoards([]);
      return;
    }
    async function loadBoards() {
      try {
        const page = await getBoards(token);
        setBoards(page.content);
        // If no board selected, pick first.
        if (!selectedBoardId && page.content.length > 0) {
          const firstId = page.content[0].id;
          setSelectedBoardId(firstId);
          navigate(`/board/${firstId}`, { replace: true });
        }
      } catch (e) {
        // For testing UI, just log.
        // eslint-disable-next-line no-console
        console.error(e);
      }
    }
    void loadBoards();
  }, [token, selectedBoardId, navigate]);

  // Allow selecting a board via ?boardId=... on root.
  useEffect(() => {
    const boardId = query.get("boardId");
    if (boardId && boardId !== selectedBoardId) {
      setSelectedBoardId(boardId);
      navigate(`/board/${boardId}`, { replace: true });
    }
    // We intentionally omit selectedBoardId to avoid loops here.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, navigate]);

  const handleUserChange = (username: string) => {
    setCurrentUser(username);
  };

  const handleBoardChange = (boardId: string) => {
    if (!boardId) {
      setSelectedBoardId(null);
      navigate("/", { replace: true });
      return;
    }
    setSelectedBoardId(boardId);
    navigate(`/board/${boardId}`);
  };

  return (
    <div className="app-root">
      <Header
        users={USERS}
        currentUser={currentUser}
        boards={boards}
        selectedBoardId={selectedBoardId}
        loginError={loginError}
        onUserChange={handleUserChange}
        onBoardChange={handleBoardChange}
        wsConnected={wsConnected}
      />

      <Routes>
        <Route
          path="/"
          element={
            selectedBoardId ? (
              <Navigate to={`/board/${selectedBoardId}`} replace />
            ) : (
              <BoardPage token={token} onWebSocketStatusChange={setWsConnected} />
            )
          }
        />
        <Route
          path="/board/:boardId"
          element={
            <BoardPage
              token={token}
              onWebSocketStatusChange={setWsConnected}
            />
          }
        />
      </Routes>
    </div>
  );
}

