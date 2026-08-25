import { describe, it, expect, vi, afterEach } from "vitest";
import { getSwipeDeck, recordSwipe } from "./swipes";

describe("swipes api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches the deck scoped to the offered item, with credentials included", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await getSwipeDeck(7);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/swipes/deck?offeredItemId=7");
    expect(init.credentials).toBe("include");
  });

  it("posts a swipe decision with the offered item as JSON", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ matched: false }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    await recordSwipe(7, 42, "LIKE");

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/swipes");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(JSON.stringify({ offeredItemId: 7, clothingItemId: 42, decision: "LIKE" }));
    expect((init.headers as Headers).get("Content-Type")).toBe("application/json");
    expect(init.credentials).toBe("include");
  });
});
