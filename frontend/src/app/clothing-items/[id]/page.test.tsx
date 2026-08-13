import { describe, it, expect, vi, afterEach } from "vitest";
import { render, waitFor } from "@testing-library/react";
import { AuthProvider } from "@/components/AuthProvider";
import ClothingItemDetailPage from "./page";

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn(), back: vi.fn() }),
  useParams: () => ({ id: "1" }),
}));

describe("ClothingItemDetailPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
  });

  it("redirects to /login when not authenticated", async () => {
    render(
      <AuthProvider>
        <ClothingItemDetailPage />
      </AuthProvider>,
    );

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });
});
