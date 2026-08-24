"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { listMyClothingItems, type ClothingItem } from "@/lib/api/clothingItems";
import { listMatches, type Match } from "@/lib/api/matches";
import { listMessages, type Message } from "@/lib/api/messages";
import { getSwipeDeck } from "@/lib/api/swipes";
import { getSwap, listSwapHistory, type Swap, type SwapHistoryEntry } from "@/lib/api/swaps";
import { Card } from "@/components/Card";
import { ButtonLink } from "@/components/Button";
import { mediaUrl } from "@/lib/media";

type DashboardMatch = {
  match: Match;
  swap: Swap | null;
  lastMessage: Message | null;
};

function ItemThumbnail({ item, size = 80 }: { item: ClothingItem; size?: number }) {
  return (
    <div
      className="relative shrink-0 overflow-hidden rounded border border-border bg-surface"
      style={{ width: size, height: size }}
    >
      <Image
        src={mediaUrl(item.imageUrl)}
        alt={item.name}
        fill
        sizes={`${size}px`}
        className="object-cover"
      />
    </div>
  );
}

export default function HomePage() {
  const { token, user, isLoading } = useAuth();

  const [items, setItems] = useState<ClothingItem[] | null>(null);
  const [itemsError, setItemsError] = useState<string | null>(null);

  const [dashboardMatches, setDashboardMatches] = useState<DashboardMatch[] | null>(null);
  const [dashboardError, setDashboardError] = useState<string | null>(null);

  const [history, setHistory] = useState<SwapHistoryEntry[] | null>(null);
  const [historyError, setHistoryError] = useState<string | null>(null);

  const [swipeTeaserOfferedItem, setSwipeTeaserOfferedItem] = useState<ClothingItem | null>(null);
  const [swipeTeaser, setSwipeTeaser] = useState<ClothingItem[] | null>(null);
  const [swipeTeaserError, setSwipeTeaserError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    listMyClothingItems(token)
      .then(setItems)
      .catch(() => setItemsError("Couldn't load your items."));
  }, [token]);

  useEffect(() => {
    if (!token) {
      return;
    }
    let cancelled = false;
    listMatches(token)
      .then(async (matches) => {
        const enriched = await Promise.all(
          matches.map(async (match) => {
            const [swap, messages] = await Promise.all([
              getSwap(token, match.id).catch(() => null),
              listMessages(token, match.id).catch(() => null),
            ]);
            return {
              match,
              swap,
              lastMessage: messages && messages.length > 0 ? messages[messages.length - 1] : null,
            };
          }),
        );
        if (!cancelled) {
          setDashboardMatches(enriched);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDashboardError("Couldn't load your matches.");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  useEffect(() => {
    if (!token) {
      return;
    }
    listSwapHistory(token)
      .then(setHistory)
      .catch(() => setHistoryError("Couldn't load your swap history."));
  }, [token]);

  useEffect(() => {
    if (!token || !items) {
      return;
    }
    const offeredItem = items.find((item) => item.status === "AVAILABLE");
    if (!offeredItem) {
      return;
    }
    getSwipeDeck(token, offeredItem.id)
      .then((deck) => {
        setSwipeTeaserOfferedItem(offeredItem);
        setSwipeTeaser(deck.slice(0, 4));
      })
      .catch(() => setSwipeTeaserError("Couldn't load new items to swipe on."));
  }, [token, items]);

  if (isLoading) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-muted">Loading…</p>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4 text-center">
        <h1 className="text-3xl font-semibold">ThreadTrades</h1>
        <p className="max-w-sm text-muted">
          Swap clothes with people nearby. Log in or create an account to get started.
        </p>
        <div className="flex gap-3">
          <ButtonLink href="/login" variant="primary">
            Log in
          </ButtonLink>
          <ButtonLink href="/register" variant="secondary">
            Register
          </ButtonLink>
        </div>
      </div>
    );
  }

  const needsAction = (dashboardMatches ?? []).filter(
    (dm): dm is DashboardMatch & { swap: Swap } =>
      dm.swap !== null && dm.swap.status === "PENDING" && !dm.swap.iAccepted,
  );

  const recentThreads = (dashboardMatches ?? [])
    .filter((dm): dm is DashboardMatch & { lastMessage: Message } => dm.lastMessage !== null)
    .sort((a, b) => new Date(b.lastMessage.sentAt).getTime() - new Date(a.lastMessage.sentAt).getTime())
    .slice(0, 5);

  const recentCompleted = (history ?? []).slice(0, 3);
  const hasAvailableItem = items?.some((item) => item.status === "AVAILABLE") ?? false;

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-8 px-4 py-10">
      <h1 className="text-2xl font-semibold">Welcome, {user.name}</h1>

      {dashboardError && <p className="text-sm text-red-600">{dashboardError}</p>}

      {needsAction.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-medium">Swaps needing your action</h2>
          <ul className="flex flex-col gap-3">
            {needsAction.map(({ match, swap }) => (
              <Card as="li" key={match.id} className="flex items-center gap-4">
                <ItemThumbnail item={match.myItem} />
                <div className="min-w-0 flex-1">
                  <p className="font-medium">{match.otherUserName}</p>
                  <Link
                    href={`/matches/${match.id}`}
                    className="block truncate text-sm text-muted underline"
                  >
                    {swap.otherAccepted
                      ? `${match.otherUserName} wants to complete this swap`
                      : "Ready to confirm this swap"}
                  </Link>
                </div>
                <ItemThumbnail item={match.otherItem} />
              </Card>
            ))}
          </ul>
        </section>
      )}

      <section>
        <h2 className="mb-1 text-lg font-medium">Recently active</h2>
        {dashboardMatches === null && !dashboardError && (
          <p className="text-sm text-muted">Loading…</p>
        )}
        {dashboardMatches !== null && recentThreads.length === 0 && (
          <p className="text-sm text-muted">No conversations yet.</p>
        )}
        {recentThreads.length > 0 && (
          <ul className="flex flex-col gap-3">
            {recentThreads.map(({ match, lastMessage }) => (
              <Card as="li" key={match.id}>
                <Link href={`/matches/${match.id}`} className="flex flex-col gap-1">
                  <span className="font-medium">{match.otherUserName}</span>
                  <span className="truncate text-sm text-muted">{lastMessage.content}</span>
                </Link>
              </Card>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-lg font-medium">Recently completed swaps</h2>
        {historyError && <p className="text-sm text-red-600">{historyError}</p>}
        {history === null && !historyError && <p className="text-sm text-muted">Loading…</p>}
        {history !== null && recentCompleted.length === 0 && (
          <p className="text-sm text-muted">No completed swaps yet.</p>
        )}
        {recentCompleted.length > 0 && (
          <ul className="flex flex-col gap-3">
            {recentCompleted.map((entry) => (
              <Card as="li" key={entry.matchId} className="flex items-center gap-4">
                <ItemThumbnail item={entry.myItem} />
                <Link href={`/matches/${entry.matchId}`} className="min-w-0 flex-1">
                  <span className="block font-medium">{entry.otherUserName}</span>
                  <span className="block truncate text-sm text-muted">
                    {entry.myItem.name} for {entry.otherItem.name}
                  </span>
                </Link>
                <ItemThumbnail item={entry.otherItem} />
              </Card>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-lg font-medium">New items to swipe on</h2>
        {swipeTeaserError && <p className="text-sm text-red-600">{swipeTeaserError}</p>}
        {!swipeTeaserError && swipeTeaser && swipeTeaser.length > 0 && (
          <Link href="/swipe" className="flex items-center gap-3">
            {swipeTeaser.map((item) => (
              <ItemThumbnail key={item.id} item={item} size={64} />
            ))}
            <span className="text-sm text-accent underline">See more</span>
          </Link>
        )}
        {!swipeTeaserError && items !== null && !hasAvailableItem && (
          <div className="flex flex-col items-center gap-3 py-6 text-center">
            <p className="text-sm text-muted">Upload an item to start swiping.</p>
            <Link href="/clothing-items/upload" className="text-sm text-accent underline">
              Upload an item
            </Link>
          </div>
        )}
        {!swipeTeaserError && swipeTeaserOfferedItem && swipeTeaser && swipeTeaser.length === 0 && (
          <div className="flex flex-col items-center gap-3 py-6 text-center">
            <p className="text-sm text-muted">Nothing new to swipe on right now — check back soon.</p>
            <Link href="/swipe" className="text-sm text-accent underline">
              Go to swipe
            </Link>
          </div>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-lg font-medium">Your items</h2>
        {itemsError && <p className="text-sm text-red-600">{itemsError}</p>}
        {items && items.length === 0 && (
          <p className="text-sm text-muted">You haven&apos;t uploaded any items yet.</p>
        )}
        {items && items.length > 0 && (
          <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            {items.map((item) => (
              <li key={item.id} className="flex flex-col gap-1">
                <Link href={`/clothing-items/${item.id}`}>
                  <div className="relative aspect-square w-full overflow-hidden rounded bg-surface">
                    <Image
                      src={mediaUrl(item.imageUrl)}
                      alt={item.name}
                      fill
                      sizes="(max-width: 640px) 50vw, 33vw"
                      className="object-cover"
                    />
                  </div>
                </Link>
                <span className="text-sm">{item.name}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
