"use client";

import { useEffect, useState } from "react";
import { Pagination } from "@/components/Pagination";
import { deleteAdminMessage, listAdminMessages, type PagedResponse } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import type { Message } from "@/lib/api/messages";
import { Card } from "@/components/Card";
import { Button } from "@/components/Button";

export default function AdminMessagesPage() {
  const [result, setResult] = useState<PagedResponse<Message> | null>(null);
  const [page, setPage] = useState(0);
  const [matchId, setMatchId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  function load() {
    listAdminMessages(page, 20, matchId ? Number(matchId) : undefined)
      .then((r) => {
        setResult(r);
        setError(null);
      })
      .catch(() => setError("Couldn't load messages."));
  }

  useEffect(load, [page, matchId]);

  async function handleDelete(id: number) {
    setActionError(null);
    try {
      await deleteAdminMessage(id);
      load();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't delete this message.");
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-medium">Messages</h2>
        <input
          type="number"
          placeholder="Filter by match id"
          value={matchId}
          onChange={(event) => {
            setPage(0);
            setMatchId(event.target.value);
          }}
          className="w-48 rounded border border-border bg-transparent px-3 py-1.5 text-sm"
        />
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}
      {actionError && <p className="text-sm text-red-600">{actionError}</p>}

      {result && (
        <ul className="flex flex-col gap-2">
          {result.content.map((message) => (
            <Card as="li" key={message.id} className="flex items-start justify-between gap-3 text-sm">
              <div className="min-w-0">
                <p className="text-xs text-muted">
                  Match #{message.matchId} · sender #{message.senderId} ·{" "}
                  {new Date(message.sentAt).toLocaleString()}
                </p>
                <p className="break-words">{message.content}</p>
              </div>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={() => handleDelete(message.id)}
                className="shrink-0"
              >
                Delete
              </Button>
            </Card>
          ))}
          {result.content.length === 0 && <p className="py-4 text-center text-sm text-muted">No messages found.</p>}
        </ul>
      )}

      {result && <Pagination page={result.page} totalPages={result.totalPages} onPageChange={setPage} />}
    </div>
  );
}
