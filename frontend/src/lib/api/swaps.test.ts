import { describe, it, expect, vi, afterEach } from "vitest";
import {
  getSwap,
  acceptSwap,
  rejectSwap,
  addExtraItem,
  removeExtraItem,
  setCashTerm,
  clearCashTerm,
  listOtherUserSwapItems,
  listSwapHistory,
} from "./swaps";

describe("swaps api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches the swap for a match, with credentials included", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({}), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await getSwap(7);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/matches/7/swap");
    expect(init.credentials).toBe("include");
  });

  it("accepts a swap", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({}), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await acceptSwap(7);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/matches/7/swap/accept");
    expect(init.method).toBe("PATCH");
  });

  it("rejects a swap", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({}), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await rejectSwap(7);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/matches/7/swap/reject");
    expect(init.method).toBe("PATCH");
  });

  it("adds an extra item as JSON", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({}), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await addExtraItem(7, 42);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/matches/7/swap/extra-items");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(JSON.stringify({ clothingItemId: 42 }));
    expect((init.headers as Headers).get("Content-Type")).toBe("application/json");
    expect(init.credentials).toBe("include");
  });

  it("fetches the other participant's requestable items", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await listOtherUserSwapItems(7);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/matches/7/swap/other-items");
    expect(init.credentials).toBe("include");
  });

  it("removes an extra item by clothing item id", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({}), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await removeExtraItem(7, 42);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/matches/7/swap/extra-items/42");
    expect(init.method).toBe("DELETE");
  });

  it("sets a cash term as JSON", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({}), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await setCashTerm(7, 1500, true);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/matches/7/swap/cash");
    expect(init.method).toBe("PUT");
    expect(init.body).toBe(JSON.stringify({ amountPence: 1500, iOwe: true }));
    expect((init.headers as Headers).get("Content-Type")).toBe("application/json");
  });

  it("clears the cash term", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({}), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await clearCashTerm(7);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/matches/7/swap/cash");
    expect(init.method).toBe("DELETE");
  });

  it("fetches swap history", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await listSwapHistory();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/swaps/history");
    expect(init.credentials).toBe("include");
  });
});
