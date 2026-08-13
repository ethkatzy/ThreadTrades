"use client";

import type { Client } from "@stomp/stompjs";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { ApiError } from "@/lib/api/client";
import { listMatches, type Match } from "@/lib/api/matches";
import { listMessages, sendMessage, type Message } from "@/lib/api/messages";
import { connectMatchSocket } from "@/lib/ws/matchSocket";

export default function MatchThreadPage() {
  const { token, user, isLoading } = useAuth();
  const router = useRouter();
  const params = useParams<{ matchId: string }>();
  const matchId = Number(params.matchId);

  const [match, setMatch] = useState<Match | null>(null);
  const [messages, setMessages] = useState<Message[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [draft, setDraft] = useState("");
  const [connected, setConnected] = useState(false);
  const [isSending, setIsSending] = useState(false);

  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [isLoading, user, router]);

  useEffect(() => {
    if (!token || Number.isNaN(matchId)) {
      return;
    }
    listMatches(token)
      .then((matches) => setMatch(matches.find((m) => m.id === matchId) ?? null))
      .catch(() => setError("Couldn't load this match."));
    listMessages(token, matchId)
      .then(setMessages)
      .catch(() => setError("Couldn't load messages."));
  }, [token, matchId]);

  useEffect(() => {
    if (!token || Number.isNaN(matchId)) {
      return;
    }
    let client: Client | null = connectMatchSocket({
      token,
      matchId,
      onMessage: (message) => setMessages((prev) => (prev ? [...prev, message] : [message])),
      onConnectedChange: setConnected,
    });
    return () => {
      client?.deactivate();
      client = null;
    };
  }, [token, matchId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: "end" });
  }, [messages]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const content = draft.trim();
    if (!token || !content || isSending) {
      return;
    }
    setIsSending(true);
    setError(null);
    try {
      await sendMessage(token, matchId, content);
      setDraft("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't send that message.");
    } finally {
      setIsSending(false);
    }
  }

  if (isLoading || !user) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-4 px-4 py-6">
      <div className="flex items-center justify-between border-b border-black/10 pb-3 dark:border-white/20">
        <button type="button" onClick={() => router.back()} className="text-sm underline">
          ← Back
        </button>
        {match && (
          <p className="text-sm font-medium">
            {match.otherUserName} · {match.myItem.name} for {match.otherItem.name}
          </p>
        )}
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}
      {messages === null && !error && (
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading messages…</p>
      )}
      {messages && messages.length === 0 && (
        <p className="text-sm text-zinc-600 dark:text-zinc-400">
          No messages yet. Say hello!
        </p>
      )}

      <div className="flex flex-1 flex-col gap-2">
        {messages?.map((message) => {
          const mine = message.senderId === user.id;
          return (
            <div key={message.id} className={`flex ${mine ? "justify-end" : "justify-start"}`}>
              <p
                className={`max-w-[75%] rounded-lg px-3 py-2 text-sm ${
                  mine
                    ? "bg-foreground text-background"
                    : "bg-zinc-100 dark:bg-zinc-900"
                }`}
              >
                {message.content}
              </p>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      <form onSubmit={handleSubmit} className="flex gap-2 border-t border-black/10 pt-3 dark:border-white/20">
        <input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder={connected ? "Type a message…" : "Connecting…"}
          disabled={!connected || isSending}
          className="flex-1 rounded border border-black/10 px-3 py-2 text-sm disabled:opacity-50 dark:border-white/20"
        />
        <button
          type="submit"
          disabled={!connected || isSending || !draft.trim()}
          className="rounded bg-foreground px-4 py-2 text-sm text-background disabled:opacity-50"
        >
          Send
        </button>
      </form>
    </div>
  );
}
