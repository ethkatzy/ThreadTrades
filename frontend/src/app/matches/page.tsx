"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { listMatches, type Match } from "@/lib/api/matches";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export default function MatchesPage() {
  const { token, user, isLoading } = useAuth();
  const router = useRouter();

  const [matches, setMatches] = useState<Match[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [isLoading, user, router]);

  useEffect(() => {
    if (!token) {
      return;
    }
    listMatches(token)
      .then(setMatches)
      .catch(() => setError("Couldn't load your matches."));
  }, [token]);

  if (isLoading || !user) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 py-10">
      <h1 className="text-2xl font-semibold">Matches</h1>

      {error && <p className="text-sm text-red-600">{error}</p>}
      {matches === null && !error && (
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading matches…</p>
      )}
      {matches && matches.length === 0 && (
        <div className="flex flex-col items-center gap-3 py-10 text-center">
          <p className="text-sm text-zinc-600 dark:text-zinc-400">
            No matches yet. Go swipe on some items!
          </p>
          <Link href="/swipe" className="text-sm underline">
            Swipe
          </Link>
        </div>
      )}

      {matches && matches.length > 0 && (
        <ul className="flex flex-col gap-4">
          {matches.map((match) => (
            <li
              key={match.id}
              className="flex items-center gap-4 rounded-lg border border-black/10 p-3 dark:border-white/20"
            >
              <Link href={`/clothing-items/${match.otherItem.id}`} className="shrink-0">
                <div className="relative h-20 w-20 overflow-hidden rounded bg-zinc-100 dark:bg-zinc-900">
                  <Image
                    src={`${API_BASE_URL}${match.otherItem.imageUrl}`}
                    alt={match.otherItem.name}
                    fill
                    sizes="80px"
                    className="object-cover"
                  />
                </div>
              </Link>
              <div className="min-w-0 flex-1">
                <Link href={`/matches/${match.id}/user`} className="font-medium underline">
                  {match.otherUserName}
                </Link>
                <Link
                  href={`/matches/${match.id}`}
                  className="block truncate text-sm text-zinc-600 dark:text-zinc-400"
                >
                  Matched on {match.otherItem.name} for your {match.myItem.name}
                </Link>
              </div>
              <Link href={`/clothing-items/${match.myItem.id}`} className="shrink-0">
                <div className="relative h-20 w-20 overflow-hidden rounded bg-zinc-100 dark:bg-zinc-900">
                  <Image
                    src={`${API_BASE_URL}${match.myItem.imageUrl}`}
                    alt={match.myItem.name}
                    fill
                    sizes="80px"
                    className="object-cover"
                  />
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
