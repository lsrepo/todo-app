import { useEffect, useRef, useState } from "react";

export interface OutboxMessage {
  type: "create" | "edit" | "delete";
  resource: "task" | "board";
  id: string;
  key: string;
  value: string;
}

function parseOutboxMessage(raw: string): OutboxMessage | null {
  const parts = raw.split(";");
  const data: Record<string, string> = {};

  for (const part of parts) {
    const idx = part.indexOf("=");
    if (idx === -1) continue;
    const key = part.slice(0, idx).trim();
    let value = part.slice(idx + 1);
    // Unescape \; and \=
    value = value.replace(/\\;/g, ";").replace(/\\=/g, "=");
    data[key] = value;
  }

  const type = data["type"];
  const resource = data["resource"];
  const id = data["id"];
  const key = data["key"];
  const value = data["value"] ?? "";

  if (!type || !resource || !id || !key) {
    return null;
  }

  if (type !== "create" && type !== "edit" && type !== "delete") {
    return null;
  }

  if (resource !== "task" && resource !== "board") {
    return null;
  }

  return { type, resource, id, key, value };
}

export function useBoardWebSocket(
  boardId: string | undefined,
  token: string | null,
  onMessage: (msg: OutboxMessage) => void
): { connected: boolean } {
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const onMessageRef = useRef(onMessage);
  const hasConnectedRef = useRef(false);

  // Keep latest handler in a ref so the WebSocket effect doesn't have to
  // re-run whenever the callback identity changes.
  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    if (!boardId || !token) {
      // If board/token disappear, ensure any existing socket is closed.
      if (wsRef.current) {
        // eslint-disable-next-line no-console
        console.log("[WS] Closing existing socket because boardId/token missing", {
          boardId,
          hasToken: !!token
        });
        wsRef.current.close();
        wsRef.current = null;
      }
      hasConnectedRef.current = false;
      return;
    }

    // For a given BoardPage mount and boardId/token pair, only establish
    // one WebSocket connection. If React re-runs this effect with the same
    // values, we skip opening another socket.
    if (hasConnectedRef.current) {
      return;
    }

    // Always connect to backend WebSocket on localhost:8088
    const wsUrl = `ws://localhost:8088/ws/board/${boardId}`;
    // eslint-disable-next-line no-console
    console.log("[WS] Opening WebSocket", { boardId, wsUrl });

    // Close any existing socket before opening a new one for this board.
    if (wsRef.current) {
      // eslint-disable-next-line no-console
      console.log("[WS] Closing previous WebSocket before opening new one", {
        previousReadyState: wsRef.current.readyState
      });
      wsRef.current.close();
      wsRef.current = null;
    }

    const ws = new WebSocket(wsUrl, ["board-v1", token]);
    wsRef.current = ws;
    hasConnectedRef.current = true;

    ws.onopen = () => {
      // eslint-disable-next-line no-console
      console.log("[WS] WebSocket opened", { boardId });
      setConnected(true);
    };

    ws.onclose = (event) => {
      // eslint-disable-next-line no-console
      console.log("[WS] WebSocket closed", {
        boardId,
        code: event.code,
        reason: event.reason
      });
      setConnected(false);
    };

    ws.onerror = (event) => {
      // eslint-disable-next-line no-console
      console.error("[WS] WebSocket error", { boardId, event });
      setConnected(false);
    };

    ws.onmessage = (event) => {
      if (typeof event.data === "string") {
        // eslint-disable-next-line no-console
        console.log("[WS] Message received", { boardId, raw: event.data });
        const parsed = parseOutboxMessage(event.data);
        if (parsed) {
          // eslint-disable-next-line no-console
          console.log("[WS] Parsed message", parsed);
          onMessageRef.current(parsed);
        }
      }
    };

    return () => {
      // eslint-disable-next-line no-console
      console.log("[WS] Cleaning up WebSocket effect, closing socket", { boardId });
      ws.close();
      if (wsRef.current === ws) {
        wsRef.current = null;
      }
      hasConnectedRef.current = false;
    };
  }, [boardId, token]);

  return { connected };
}

