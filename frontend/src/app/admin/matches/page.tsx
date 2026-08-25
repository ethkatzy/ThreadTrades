"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Pagination } from "@/components/Pagination";
import { listAdminMatches, type AdminMatch, type PagedResponse } from "@/lib/api/admin";
import { StatusBadge, swapStatusTone } from "@/components/StatusBadge";

export default function AdminMatchesPage() {
  const [result, setResult] = useState<PagedResponse<AdminMatch> | null>(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listAdminMatches(page)
      .then((r) => {
        setResult(r);
        setError(null);
      })
      .catch(() => setError("Couldn't load matches."));
  }, [page]);

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-medium">Matches</h2>

      {error && <p className="text-sm text-red-600">{error}</p>}

      {result && (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-muted">
                <th className="py-2 pr-4">Users</th>
                <th className="py-2 pr-4">Swap status</th>
                <th className="py-2">Matched</th>
              </tr>
            </thead>
            <tbody>
              {result.content.map((match) => (
                <tr key={match.id} className="border-b border-border/50">
                  <td className="py-2 pr-4">
                    <Link href={`/admin/matches/${match.id}`} className="text-accent underline">
                      {match.userAUsername} &amp; {match.userBUsername}
                    </Link>
                  </td>
                  <td className="py-2 pr-4">
                    <StatusBadge
                      label={match.swapStatus ?? "No swap yet"}
                      tone={match.swapStatus ? swapStatusTone(match.swapStatus) : "neutral"}
                    />
                  </td>
                  <td className="py-2">{new Date(match.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
              {result.content.length === 0 && (
                <tr>
                  <td colSpan={3} className="py-4 text-center text-muted">
                    No matches found.
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
