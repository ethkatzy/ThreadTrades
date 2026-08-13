import { describe, it, expect, vi, afterEach } from "vitest";
import { listMessages, sendMessage } from "./messages";

describe("messages api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches a match's message history with auth header", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await listMessages("abc123", 7);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/matches/7/messages");
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer abc123");
  });

  it("posts a message as JSON", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ id: 1 }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    await sendMessage("abc123", 7, "Hi there");

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/matches/7/messages");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(JSON.stringify({ content: "Hi there" }));
    expect((init.headers as Headers).get("Content-Type")).toBe("application/json");
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer abc123");
  });
});
