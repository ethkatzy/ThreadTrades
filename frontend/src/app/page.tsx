"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { listMyClothingItems, type ClothingItem } from "@/lib/api/clothingItems";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const ENTRY_POINTS = [
  { href: "/clothing-items/upload", label: "Upload an item" },
  { href: "/swipe", label: "Swipe" },
  { href: "/matches", label: "Matches" },
  { href: "/profile", label: "Profile" },
];

export default function HomePage() {
  const { token, user, isLoading, logout } = useAuth();
  const [items, setItems] = useState<ClothingItem[] | null>(null);
  const [itemsError, setItemsError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    listMyClothingItems(token)
      .then(setItems)
      .catch(() => setItemsError("Couldn't load your items."));
  }, [token]);

  if (isLoading) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4 text-center">
        <h1 className="text-3xl font-semibold">ThreadTrades</h1>
        <p className="max-w-sm text-zinc-600 dark:text-zinc-400">
          Swap clothes with people nearby. Log in or create an account to get started.
        </p>
        <div className="flex gap-3">
          <Link href="/login" className="rounded bg-foreground px-4 py-2 text-background">
            Log in
          </Link>
          <Link
            href="/register"
            className="rounded border border-black/10 px-4 py-2 dark:border-white/20"
          >
            Register
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-8 px-4 py-10">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Welcome, {user.name}</h1>
        <button onClick={logout} className="text-sm underline">
          Log out
        </button>
      </div>

      <nav className="flex flex-wrap gap-3">
        {ENTRY_POINTS.map((entry) => (
          <Link
            key={entry.href}
            href={entry.href}
            className="rounded border border-black/10 px-4 py-2 text-sm dark:border-white/20"
          >
            {entry.label}
          </Link>
        ))}
      </nav>

      <section>
        <h2 className="mb-3 text-lg font-medium">Your items</h2>
        {itemsError && <p className="text-sm text-red-600">{itemsError}</p>}
        {items && items.length === 0 && (
          <p className="text-sm text-zinc-600 dark:text-zinc-400">
            You haven&apos;t uploaded any items yet.
          </p>
        )}
        {items && items.length > 0 && (
          <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            {items.map((item) => (
              <li key={item.id} className="flex flex-col gap-1">
                <div className="relative aspect-square w-full overflow-hidden rounded bg-zinc-100 dark:bg-zinc-900">
                  <Image
                    src={`${API_BASE_URL}${item.imageUrl}`}
                    alt={item.name}
                    fill
                    sizes="(max-width: 640px) 50vw, 33vw"
                    className="object-cover"
                  />
                </div>
                <span className="text-sm">{item.name}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
