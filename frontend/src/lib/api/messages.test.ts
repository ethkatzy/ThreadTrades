import { describe, it, expect, vi, afterEach } from "vitest";
import { listMessages, sendMessage } from "./messages";

describe("messages api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches a match's message history with credentials included", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await listMessages(7);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/matches/7/messages");
    expect(init.credentials).toBe("include");
  });

  it("posts a message as JSON", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ id: 1 }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    await sendMessage(7, "Hi there");

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/matches/7/messages");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(JSON.stringify({ content: "Hi there" }));
    expect((init.headers as Headers).get("Content-Type")).toBe("application/json");
    expect(init.credentials).toBe("include");
  });
});
