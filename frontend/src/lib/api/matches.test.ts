import { describe, it, expect, vi, afterEach } from "vitest";
import { listMatches } from "./matches";

describe("matches api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches the caller's matches with auth header", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await listMatches("abc123");

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/matches");
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer abc123");
  });
});
