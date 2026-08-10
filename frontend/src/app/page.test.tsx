import { describe, it, expect, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { AuthProvider } from "@/components/AuthProvider";
import HomePage from "./page";

describe("HomePage", () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  it("shows login/register entry points when logged out", async () => {
    render(
      <AuthProvider>
        <HomePage />
      </AuthProvider>,
    );

    expect(await screen.findByRole("link", { name: "Log in" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Register" })).toBeInTheDocument();
  });
});
