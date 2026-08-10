import { describe, it, expect, vi, afterEach } from "vitest";
import { apiFetch, ApiError } from "./client";

describe("apiFetch", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("sends JSON content type and returns the parsed body", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await apiFetch<{ ok: boolean }>("/api/ping", {
      method: "POST",
      body: JSON.stringify({ a: 1 }),
    });

    expect(result).toEqual({ ok: true });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/ping");
    expect((init.headers as Headers).get("Content-Type")).toBe("application/json");
  });

  it("attaches a bearer token when provided", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("{}", { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await apiFetch("/api/users/me", {}, "abc123");

    const [, init] = fetchMock.mock.calls[0];
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer abc123");
  });

  it("throws an ApiError carrying the backend's message and status", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ message: "Invalid email or password" }), { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch("/api/auth/login", { method: "POST" })).rejects.toSatisfy((err: unknown) => {
      return err instanceof ApiError && err.message === "Invalid email or password" && err.status === 401;
    });
  });
});
