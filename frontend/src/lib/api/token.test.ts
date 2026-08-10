import { describe, it, expect, afterEach } from "vitest";
import { clearStoredToken, getStoredToken, setStoredToken } from "./token";

describe("token storage", () => {
  afterEach(() => {
    clearStoredToken();
  });

  it("returns null when nothing is stored", () => {
    expect(getStoredToken()).toBeNull();
  });

  it("round-trips a token through localStorage", () => {
    setStoredToken("my-token");
    expect(getStoredToken()).toBe("my-token");

    clearStoredToken();
    expect(getStoredToken()).toBeNull();
  });
});
