"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { getAdminMatch, type AdminMatch } from "@/lib/api/admin";

export default function AdminMatchDetailPage() {
  const { token } = useAuth();
  const params = useParams<{ id: string }>();
  const matchId = Number(params.id);

  const [match, setMatch] = useState<AdminMatch | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    getAdminMatch(token, matchId)
      .then(setMatch)
      .catch(() => setError("Couldn't load this match."));
  }, [token, matchId]);

  if (error) {
    return <p className="text-sm text-red-600">{error}</p>;
  }
  if (!match) {
    return <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-medium">
        {match.userAUsername} &amp; {match.userBUsername}
      </h2>
      <div className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <p className="font-medium">{match.userAUsername}</p>
          <p className="text-zinc-600 dark:text-zinc-400">offered {match.itemA.name}</p>
        </div>
        <div>
          <p className="font-medium">{match.userBUsername}</p>
          <p className="text-zinc-600 dark:text-zinc-400">offered {match.itemB.name}</p>
        </div>
      </div>
      <p className="text-sm">Matched on {new Date(match.createdAt).toLocaleString()}</p>
      <p className="text-sm">
        Swap status: {match.swapStatus ?? "No swap yet"}
        {match.swapId && (
          <>
            {" "}
            —{" "}
            <Link href={`/admin/swaps/${match.swapId}`} className="underline">
              view swap
            </Link>
          </>
        )}
      </p>
    </div>
  );
}
