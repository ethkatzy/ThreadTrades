"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { listMatches, type Match } from "@/lib/api/matches";
import { Card } from "@/components/Card";
import { Avatar } from "@/components/Avatar";

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
        <p className="text-sm text-muted">Loading…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 py-10">
      <h1 className="text-2xl font-semibold">Matches</h1>

      {error && <p className="text-sm text-red-600">{error}</p>}
      {matches === null && !error && <p className="text-sm text-muted">Loading matches…</p>}
      {matches && matches.length === 0 && (
        <div className="flex flex-col items-center gap-3 py-10 text-center">
          <p className="text-sm text-muted">No matches yet. Go swipe on some items!</p>
          <Link href="/swipe" className="text-sm text-accent underline">
            Swipe
          </Link>
        </div>
      )}

      {matches && matches.length > 0 && (
        <ul className="flex flex-col gap-4">
          {matches.map((match) => (
            <Card as="li" key={match.id} className="flex items-center gap-4">
              <Link href={`/clothing-items/${match.otherItem.id}`} className="shrink-0">
                <div className="relative h-20 w-20 overflow-hidden rounded bg-background">
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
                <Link
                  href={`/matches/${match.id}/user`}
                  className="flex items-center gap-2 font-medium hover:underline"
                >
                  <Avatar
                    src={
                      match.otherUserProfilePictureUrl
                        ? `${API_BASE_URL}${match.otherUserProfilePictureUrl}`
                        : null
                    }
                    name={match.otherUserName}
                    size={28}
                  />
                  {match.otherUserName}
                </Link>
                <Link href={`/matches/${match.id}`} className="block truncate text-sm text-muted">
                  Matched on {match.otherItem.name} for your {match.myItem.name}
                </Link>
              </div>
              <Link href={`/clothing-items/${match.myItem.id}`} className="shrink-0">
                <div className="relative h-20 w-20 overflow-hidden rounded bg-background">
                  <Image
                    src={`${API_BASE_URL}${match.myItem.imageUrl}`}
                    alt={match.myItem.name}
                    fill
                    sizes="80px"
                    className="object-cover"
                  />
                </div>
              </Link>
            </Card>
          ))}
        </ul>
      )}
    </div>
  );
}
