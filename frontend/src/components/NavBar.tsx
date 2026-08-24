"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/components/AuthProvider";

const NAV_LINKS = [
  { href: "/clothing-items/upload", label: "Upload an item" },
  { href: "/swipe", label: "Swipe" },
  { href: "/matches", label: "Matches" },
  { href: "/profile", label: "Profile" },
];

export function NavBar() {
  const { user, isLoading, logout } = useAuth();
  const pathname = usePathname();

  const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

  return (
    <header className="sticky top-0 z-10 border-b border-border bg-background/90 backdrop-blur">
      <div className="mx-auto flex w-full max-w-4xl items-center justify-between gap-4 px-4 py-3">
        <Link href="/" className="text-base font-semibold tracking-tight text-accent">
          ThreadTrades
        </Link>

        {!isLoading && user && (
          <nav className="flex flex-wrap items-center gap-1 text-sm">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={`rounded px-3 py-1.5 ${
                  isActive(link.href) ? "bg-accent/15 text-accent" : "text-foreground hover:bg-surface"
                }`}
              >
                {link.label}
              </Link>
            ))}
            {user.role === "ADMIN" && (
              <Link
                href="/admin"
                className={`rounded px-3 py-1.5 ${
                  isActive("/admin") ? "bg-accent/15 text-accent" : "text-foreground hover:bg-surface"
                }`}
              >
                Admin
              </Link>
            )}
            <button
              type="button"
              onClick={logout}
              className="ml-1 rounded px-3 py-1.5 text-muted hover:bg-surface"
            >
              Log out
            </button>
          </nav>
        )}

        {!isLoading && !user && (
          <nav className="flex items-center gap-2 text-sm">
            <Link href="/login" className="rounded px-3 py-1.5 text-foreground hover:bg-surface">
              Log in
            </Link>
            <Link href="/register" className="rounded bg-accent px-3 py-1.5 text-accent-foreground">
              Register
            </Link>
          </nav>
        )}
      </div>
    </header>
  );
}
