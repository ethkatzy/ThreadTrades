"use client";

import Image from "next/image";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { getAdminClothingItem, removeClothingItem } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import type { ClothingItem } from "@/lib/api/clothingItems";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export default function AdminClothingItemDetailPage() {
  const { token } = useAuth();
  const params = useParams<{ id: string }>();
  const itemId = Number(params.id);

  const [item, setItem] = useState<ClothingItem | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!token) {
      return;
    }
    getAdminClothingItem(token, itemId)
      .then(setItem)
      .catch(() => setError("Couldn't load this item."));
  }, [token, itemId]);

  async function handleRemove() {
    if (!token) {
      return;
    }
    setIsSubmitting(true);
    setActionError(null);
    try {
      setItem(await removeClothingItem(token, itemId));
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't remove this item.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (error) {
    return <p className="text-sm text-red-600">{error}</p>;
  }
  if (!item) {
    return <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-4">
        <div className="relative h-32 w-32 shrink-0 overflow-hidden rounded bg-zinc-100 dark:bg-zinc-900">
          <Image src={`${API_BASE_URL}${item.imageUrl}`} alt={item.name} fill sizes="128px" className="object-cover" />
        </div>
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
          <dt className="text-zinc-600 dark:text-zinc-400">Name</dt>
          <dd>{item.name}</dd>
          <dt className="text-zinc-600 dark:text-zinc-400">Owner</dt>
          <dd>{item.ownerId}</dd>
          <dt className="text-zinc-600 dark:text-zinc-400">Status</dt>
          <dd>{item.status}</dd>
          <dt className="text-zinc-600 dark:text-zinc-400">Type</dt>
          <dd>{item.itemType}</dd>
          <dt className="text-zinc-600 dark:text-zinc-400">Created</dt>
          <dd>{new Date(item.createdAt).toLocaleString()}</dd>
        </dl>
      </div>

      {actionError && <p className="text-sm text-red-600">{actionError}</p>}
      {item.status !== "REMOVED" && (
        <button
          type="button"
          disabled={isSubmitting}
          onClick={handleRemove}
          className="w-fit rounded border border-black/10 px-4 py-2 text-sm disabled:opacity-50 dark:border-white/20"
        >
          Remove item
        </button>
      )}
    </div>
  );
}
