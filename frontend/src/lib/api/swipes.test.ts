import { describe, it, expect, vi, afterEach } from "vitest";
import { getSwipeDeck, recordSwipe } from "./swipes";

describe("swipes api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches the deck scoped to the offered item, with auth header", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await getSwipeDeck("abc123", 7);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/swipes/deck?offeredItemId=7");
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer abc123");
  });

  it("posts a swipe decision with the offered item as JSON", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ matched: false }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    await recordSwipe("abc123", 7, 42, "LIKE");

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/swipes");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(JSON.stringify({ offeredItemId: 7, clothingItemId: 42, decision: "LIKE" }));
    expect((init.headers as Headers).get("Content-Type")).toBe("application/json");
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer abc123");
  });
});
