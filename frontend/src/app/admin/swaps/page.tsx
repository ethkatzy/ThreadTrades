"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Pagination } from "@/components/Pagination";
import { listAdminSwaps, type AdminSwap } from "@/lib/api/admin";
import type { PagedResponse } from "@/lib/api/admin";
import type { SwapStatus } from "@/lib/api/swaps";
import { StatusBadge, swapStatusTone } from "@/components/StatusBadge";

const STATUS_OPTIONS: Array<SwapStatus | ""> = ["", "PENDING", "ACCEPTED", "REJECTED"];

export default function AdminSwapsPage() {
  const { token } = useAuth();
  const [result, setResult] = useState<PagedResponse<AdminSwap> | null>(null);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<SwapStatus | "">("PENDING");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    listAdminSwaps(token, page, 20, status || undefined)
      .then((r) => {
        setResult(r);
        setError(null);
      })
      .catch(() => setError("Couldn't load swaps."));
  }, [token, page, status]);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-medium">Swaps</h2>
        <select
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value as SwapStatus | "");
          }}
          className="rounded border border-border bg-transparent px-3 py-1.5 text-sm"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option} value={option} className="bg-white text-black">
              {option || "All statuses"}
            </option>
          ))}
        </select>
      </div>
      {status === "PENDING" && (
        <p className="text-xs text-muted">Sorted oldest-first so stuck swaps surface at the top.</p>
      )}

      {error && <p className="text-sm text-red-600">{error}</p>}

      {result && (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-muted">
                <th className="py-2 pr-4">Match</th>
                <th className="py-2 pr-4">Status</th>
                <th className="py-2">Last updated</th>
              </tr>
            </thead>
            <tbody>
              {result.content.map((swap) => (
                <tr key={swap.id} className="border-b border-border/50">
                  <td className="py-2 pr-4">
                    <Link href={`/admin/swaps/${swap.id}`} className="text-accent underline">
                      Match #{swap.matchId}
                    </Link>
                  </td>
                  <td className="py-2 pr-4">
                    <StatusBadge label={swap.status} tone={swapStatusTone(swap.status)} />
                  </td>
                  <td className="py-2">{new Date(swap.updatedAt).toLocaleString()}</td>
                </tr>
              ))}
              {result.content.length === 0 && (
                <tr>
                  <td colSpan={3} className="py-4 text-center text-muted">
                    No swaps found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {result && <Pagination page={result.page} totalPages={result.totalPages} onPageChange={setPage} />}
    </div>
  );
}
