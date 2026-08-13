import { describe, it, expect, vi, afterEach } from "vitest";
import { getClothingItem, uploadClothingItem } from "./clothingItems";

describe("getClothingItem", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches a single item by id with auth header", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ id: 5 }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await getClothingItem("abc123", 5);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/clothing-items/5");
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer abc123");
  });
});

describe("uploadClothingItem", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("posts FormData without setting a Content-Type header", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ id: 1 }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    const formData = new FormData();
    formData.append("name", "Denim Jacket");

    await uploadClothingItem("abc123", formData);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/clothing-items");
    expect(init.method).toBe("POST");
    expect(init.body).toBe(formData);
    // The browser must set its own multipart boundary (AUDIT.md #8) --
    // setting Content-Type manually corrupts the upload.
    expect((init.headers as Headers).has("Content-Type")).toBe(false);
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer abc123");
  });
});
