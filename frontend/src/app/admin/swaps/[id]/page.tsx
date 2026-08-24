"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { forceResolveSwap, getAdminSwap, type AdminSwap } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";

export default function AdminSwapDetailPage() {
  const { token } = useAuth();
  const params = useParams<{ id: string }>();
  const swapId = Number(params.id);

  const [swap, setSwap] = useState<AdminSwap | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!token) {
      return;
    }
    getAdminSwap(token, swapId)
      .then(setSwap)
      .catch(() => setError("Couldn't load this swap."));
  }, [token, swapId]);

  async function handleForceResolve(target: "ACCEPTED" | "REJECTED") {
    if (!token) {
      return;
    }
    setIsSubmitting(true);
    setActionError(null);
    try {
      setSwap(await forceResolveSwap(token, swapId, target));
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't force-resolve this swap.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (error) {
    return <p className="text-sm text-red-600">{error}</p>;
  }
  if (!swap) {
    return <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-medium">Swap #{swap.id}</h2>
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
        <dt className="text-zinc-600 dark:text-zinc-400">Match</dt>
        <dd>
          <Link href={`/admin/matches/${swap.matchId}`} className="underline">
            #{swap.matchId}
          </Link>
        </dd>
        <dt className="text-zinc-600 dark:text-zinc-400">Status</dt>
        <dd>{swap.status}</dd>
        <dt className="text-zinc-600 dark:text-zinc-400">Accepted by A</dt>
        <dd>{swap.acceptedByUserA ? "Yes" : "No"}</dd>
        <dt className="text-zinc-600 dark:text-zinc-400">Accepted by B</dt>
        <dd>{swap.acceptedByUserB ? "Yes" : "No"}</dd>
        <dt className="text-zinc-600 dark:text-zinc-400">Last updated</dt>
        <dd>{new Date(swap.updatedAt).toLocaleString()}</dd>
      </dl>

      {actionError && <p className="text-sm text-red-600">{actionError}</p>}
      <div className="flex gap-3">
        <button
          type="button"
          disabled={isSubmitting || swap.status === "ACCEPTED"}
          onClick={() => handleForceResolve("ACCEPTED")}
          className="rounded border border-black/10 px-4 py-2 text-sm disabled:opacity-50 dark:border-white/20"
        >
          Force accept
        </button>
        <button
          type="button"
          disabled={isSubmitting || swap.status === "REJECTED"}
          onClick={() => handleForceResolve("REJECTED")}
          className="rounded border border-black/10 px-4 py-2 text-sm disabled:opacity-50 dark:border-white/20"
        >
          Force reject
        </button>
      </div>
    </div>
  );
}
