import type { Page } from "./types";

const API_BASE = "/api";

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  token?: string
): Promise<T> {
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };

  if (token) {
    (headers as Record<string, string>)["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(
      `Request failed with status ${response.status}: ${text || response.statusText}`
    );
  }

  if (response.status === 204) {
    // No content
    return undefined as unknown as T;
  }

  return (await response.json()) as T;
}

export type { Page };

