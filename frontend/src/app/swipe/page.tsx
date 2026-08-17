"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { RatingBadge } from "@/components/RatingBadge";
import { ApiError } from "@/lib/api/client";
import { listMyClothingItems, type ClothingItem } from "@/lib/api/clothingItems";
import { getSwipeDeck, recordSwipe, type SwipeDecision } from "@/lib/api/swipes";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const DRAG_THRESHOLD = 100;

export default function SwipePage() {
  const { token, user, isLoading } = useAuth();
  const router = useRouter();

  const [myItems, setMyItems] = useState<ClothingItem[] | null>(null);
  const [myItemsError, setMyItemsError] = useState<string | null>(null);
  const [offeredItemId, setOfferedItemId] = useState<number | null>(null);

  const [items, setItems] = useState<ClothingItem[] | null>(null);
  const [index, setIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [lastMatchItem, setLastMatchItem] = useState<ClothingItem | null>(null);

  const [dragX, setDragX] = useState(0);
  const [dragging, setDragging] = useState(false);
  const pointerStartX = useRef<number | null>(null);
  // Mirrors dragX outside React state so pointerup can read the latest value
  // even if it fires before the pointermove state update has committed.
  const dragXRef = useRef(0);

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [isLoading, user, router]);

  useEffect(() => {
    if (!token) {
      return;
    }
    listMyClothingItems(token)
      .then(setMyItems)
      .catch(() => setMyItemsError("Couldn't load your items."));
  }, [token]);

  useEffect(() => {
    if (!token || offeredItemId === null) {
      return;
    }
    // Clear the previous offered item's deck/match state before loading the new
    // one, rather than showing stale results while the new deck is in flight.
    // eslint-disable-next-line react-hooks/set-state-in-effect -- see comment above
    setItems(null);
    setLastMatchItem(null);
    getSwipeDeck(token, offeredItemId)
      .then((deck) => {
        setItems(deck);
        setIndex(0);
      })
      .catch(() => setError("Couldn't load items to swipe."));
  }, [token, offeredItemId]);

  async function handleSwipe(decision: SwipeDecision) {
    const item = items?.[index];
    if (!token || !item || offeredItemId === null || isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    setError(null);
    try {
      const result = await recordSwipe(token, offeredItemId, item.id, decision);
      setLastMatchItem(result.matched ? item : null);
      setIndex((i) => i + 1);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    } finally {
      setIsSubmitting(false);
      dragXRef.current = 0;
      setDragX(0);
    }
  }

  function handlePointerDown(event: ReactPointerEvent<HTMLDivElement>) {
    if (isSubmitting) {
      return;
    }
    event.currentTarget.setPointerCapture(event.pointerId);
    pointerStartX.current = event.clientX;
    setDragging(true);
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLDivElement>) {
    if (pointerStartX.current === null) {
      return;
    }
    dragXRef.current = event.clientX - pointerStartX.current;
    setDragX(dragXRef.current);
  }

  function handlePointerUp() {
    if (pointerStartX.current === null) {
      return;
    }
    pointerStartX.current = null;
    setDragging(false);
    const finalDragX = dragXRef.current;
    if (finalDragX > DRAG_THRESHOLD) {
      void handleSwipe("LIKE");
    } else if (finalDragX < -DRAG_THRESHOLD) {
      void handleSwipe("DISLIKE");
    } else {
      dragXRef.current = 0;
      setDragX(0);
    }
  }

  if (isLoading || !user) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>
      </div>
    );
  }

  const offeredItem = myItems?.find((item) => item.id === offeredItemId) ?? null;

  if (offeredItemId === null || !offeredItem) {
    return (
      <div className="mx-auto flex w-full max-w-md flex-1 flex-col gap-6 px-4 py-10">
        <h1 className="text-2xl font-semibold">Swipe</h1>
        <p className="text-sm text-zinc-600 dark:text-zinc-400">
          Which of your items do you want to trade? You&apos;ll see the deck of what other people
          have to offer for it.
        </p>

        {myItemsError && <p className="text-sm text-red-600">{myItemsError}</p>}
        {myItems === null && !myItemsError && (
          <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading your items…</p>
        )}
        {myItems && myItems.length === 0 && (
          <div className="flex flex-col items-center gap-3 text-center">
            <p className="text-sm text-zinc-600 dark:text-zinc-400">
              You need to upload an item before you can swipe.
            </p>
            <Link href="/clothing-items/upload" className="text-sm underline">
              Upload an item
            </Link>
          </div>
        )}
        {myItems && myItems.length > 0 && (
          <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            {myItems.map((item) => (
              <li key={item.id}>
                <button
                  type="button"
                  onClick={() => setOfferedItemId(item.id)}
                  className="flex w-full flex-col gap-1 rounded border border-black/10 p-2 text-left dark:border-white/20"
                >
                  <div className="relative aspect-square w-full overflow-hidden rounded bg-zinc-100 dark:bg-zinc-900">
                    <Image
                      src={`${API_BASE_URL}${item.imageUrl}`}
                      alt={item.name}
                      fill
                      sizes="(max-width: 640px) 50vw, 33vw"
                      className="object-cover"
                    />
                  </div>
                  <span className="text-sm">{item.name}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    );
  }

  const currentItem = items?.[index] ?? null;

  return (
    <div className="mx-auto flex w-full max-w-md flex-1 flex-col items-center gap-6 px-4 py-10">
      <div className="flex w-full items-center justify-between">
        <h1 className="text-2xl font-semibold">Swipe</h1>
        <button type="button" onClick={() => setOfferedItemId(null)} className="text-sm underline">
          Offering: {offeredItem.name} (change)
        </button>
      </div>

      {lastMatchItem && (
        <p className="rounded bg-green-100 px-4 py-2 text-sm text-green-800 dark:bg-green-950 dark:text-green-300">
          It&apos;s a match on &quot;{lastMatchItem.name}&quot;! 🎉
        </p>
      )}
      {error && <p className="text-sm text-red-600">{error}</p>}

      {items === null && <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading items…</p>}

      {items !== null && !currentItem && (
        <div className="flex flex-col items-center gap-3 text-center">
          <p className="text-sm text-zinc-600 dark:text-zinc-400">
            You&apos;re all caught up! Check back later for more items.
          </p>
          <Link href="/" className="text-sm underline">
            Back home
          </Link>
        </div>
      )}

      {currentItem && (
        <>
          <div
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerCancel={handlePointerUp}
            style={{
              transform: `translateX(${dragX}px) rotate(${dragX / 20}deg)`,
              transition: dragging ? "none" : "transform 0.2s ease",
            }}
            className="flex w-full touch-none flex-col gap-3 rounded-lg border border-black/10 bg-background p-4 select-none dark:border-white/20"
          >
            <div className="relative aspect-square w-full overflow-hidden rounded bg-zinc-100 dark:bg-zinc-900">
              <Image
                src={`${API_BASE_URL}${currentItem.imageUrl}`}
                alt={currentItem.name}
                fill
                sizes="(max-width: 640px) 100vw, 448px"
                className="object-cover"
                draggable={false}
              />
            </div>
            <div>
              <h2 className="text-lg font-medium">{currentItem.name}</h2>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">
                {[currentItem.brand, currentItem.itemType, currentItem.clothingSize]
                  .filter(Boolean)
                  .join(" • ")}
              </p>
              {currentItem.description && (
                <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-400">{currentItem.description}</p>
              )}
            </div>
          </div>

          {/* Outside the draggable card on purpose: that div captures the pointer for the
              swipe gesture, which would swallow clicks on a nested link. */}
          <RatingBadge
            userId={currentItem.ownerId}
            averageRating={currentItem.ownerAverageRating}
            reviewCount={currentItem.ownerReviewCount}
          />

          <div className="flex gap-4">
            <button
              type="button"
              onClick={() => handleSwipe("DISLIKE")}
              disabled={isSubmitting}
              className="rounded border border-black/10 px-6 py-2 text-lg disabled:opacity-50 dark:border-white/20"
              aria-label="Pass"
            >
              ✕
            </button>
            <button
              type="button"
              onClick={() => handleSwipe("LIKE")}
              disabled={isSubmitting}
              className="rounded bg-foreground px-6 py-2 text-lg text-background disabled:opacity-50"
              aria-label="Like"
            >
              ♥
            </button>
          </div>
        </>
      )}
    </div>
  );
}
