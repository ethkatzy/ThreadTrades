"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { forceResolveSwap, getAdminSwap, type AdminSwap } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Button } from "@/components/Button";
import { StatusBadge, swapStatusTone } from "@/components/StatusBadge";

export default function AdminSwapDetailPage() {
  const params = useParams<{ id: string }>();
  const swapId = Number(params.id);

  const [swap, setSwap] = useState<AdminSwap | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    getAdminSwap(swapId)
      .then(setSwap)
      .catch(() => setError("Couldn't load this swap."));
  }, [swapId]);

  async function handleForceResolve(target: "ACCEPTED" | "REJECTED") {
    setIsSubmitting(true);
    setActionError(null);
    try {
      setSwap(await forceResolveSwap(swapId, target));
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
    return <p className="text-sm text-muted">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-medium">Swap #{swap.id}</h2>
      <dl className="grid grid-cols-[auto_1fr] items-center gap-x-4 gap-y-1 text-sm">
        <dt className="text-muted">Match</dt>
        <dd>
          <Link href={`/admin/matches/${swap.matchId}`} className="text-accent underline">
            #{swap.matchId}
          </Link>
        </dd>
        <dt className="text-muted">Status</dt>
        <dd>
          <StatusBadge label={swap.status} tone={swapStatusTone(swap.status)} />
        </dd>
        <dt className="text-muted">Accepted by A</dt>
        <dd>{swap.acceptedByUserA ? "Yes" : "No"}</dd>
        <dt className="text-muted">Accepted by B</dt>
        <dd>{swap.acceptedByUserB ? "Yes" : "No"}</dd>
        <dt className="text-muted">Last updated</dt>
        <dd>{new Date(swap.updatedAt).toLocaleString()}</dd>
      </dl>

      {actionError && <p className="text-sm text-red-600">{actionError}</p>}
      <div className="flex gap-3">
        <Button
          type="button"
          variant="secondary"
          disabled={isSubmitting || swap.status === "ACCEPTED"}
          onClick={() => handleForceResolve("ACCEPTED")}
        >
          Force accept
        </Button>
        <Button
          type="button"
          variant="secondary"
          disabled={isSubmitting || swap.status === "REJECTED"}
          onClick={() => handleForceResolve("REJECTED")}
        >
          Force reject
        </Button>
      </div>
    </div>
  );
}
