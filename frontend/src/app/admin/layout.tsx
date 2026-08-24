"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/components/AuthProvider";

const ADMIN_NAV = [
  { href: "/admin", label: "Dashboard" },
  { href: "/admin/users", label: "Users" },
  { href: "/admin/clothing-items", label: "Clothing items" },
  { href: "/admin/matches", label: "Matches" },
  { href: "/admin/swaps", label: "Swaps" },
  { href: "/admin/messages", label: "Messages" },
  { href: "/admin/reviews", label: "Reviews" },
];

/**
 * The one place the admin role check happens -- every /admin/* page relies on
 * this layout for the redirect instead of repeating the per-page guard effect
 * pattern used elsewhere in the app. A moderation-data-exposure bug from a
 * missed guard on any one of ~7 pages is worse than the extra indirection.
 * This is UX only: the real enforcement is server-side (SecurityConfig
 * hasRole("ADMIN") on /api/admin/**), same trust model as the rest of the app.
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && (!user || user.role !== "ADMIN")) {
      router.replace("/");
    }
  }, [isLoading, user, router]);

  if (isLoading || !user || user.role !== "ADMIN") {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-1 flex-col gap-6 px-4 py-10">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Admin</h1>
        <Link href="/" className="text-sm underline">
          Back to app
        </Link>
      </div>
      <nav className="flex flex-wrap gap-2 border-b border-black/10 pb-4 dark:border-white/20">
        {ADMIN_NAV.map((entry) => (
          <Link
            key={entry.href}
            href={entry.href}
            className="rounded border border-black/10 px-3 py-1.5 text-sm dark:border-white/20"
          >
            {entry.label}
          </Link>
        ))}
      </nav>
      {children}
    </div>
  );
}
