import { describe, it, expect, vi, afterEach } from "vitest";
import { listMatches } from "./matches";

describe("matches api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches the caller's matches with credentials included", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await listMatches();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/matches");
    expect(init.credentials).toBe("include");
  });
});
