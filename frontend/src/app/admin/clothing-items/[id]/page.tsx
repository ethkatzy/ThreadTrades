"use client";

import Image from "next/image";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { getAdminClothingItem, removeClothingItem } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import type { ClothingItem } from "@/lib/api/clothingItems";
import { Button } from "@/components/Button";
import { StatusBadge, itemStatusTone } from "@/components/StatusBadge";
import { mediaUrl } from "@/lib/media";

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
    return <p className="text-sm text-muted">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-4">
        <div className="relative h-32 w-32 shrink-0 overflow-hidden rounded bg-surface">
          <Image src={mediaUrl(item.imageUrl)} alt={item.name} fill sizes="128px" className="object-cover" />
        </div>
        <dl className="grid grid-cols-[auto_1fr] items-center gap-x-4 gap-y-1 text-sm">
          <dt className="text-muted">Name</dt>
          <dd>{item.name}</dd>
          <dt className="text-muted">Owner</dt>
          <dd>{item.ownerId}</dd>
          <dt className="text-muted">Status</dt>
          <dd>
            <StatusBadge label={item.status} tone={itemStatusTone(item.status)} />
          </dd>
          <dt className="text-muted">Type</dt>
          <dd>{item.itemType}</dd>
          <dt className="text-muted">Created</dt>
          <dd>{new Date(item.createdAt).toLocaleString()}</dd>
        </dl>
      </div>

      {actionError && <p className="text-sm text-red-600">{actionError}</p>}
      {item.status !== "REMOVED" && (
        <Button type="button" variant="secondary" disabled={isSubmitting} onClick={handleRemove} className="w-fit">
          Remove item
        </Button>
      )}
    </div>
  );
}
