"use client";

import Image from "next/image";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { ApiError } from "@/lib/api/client";
import { getClothingItem, type ClothingItem } from "@/lib/api/clothingItems";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export default function ClothingItemDetailPage() {
  const { token, user, isLoading } = useAuth();
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const itemId = Number(params.id);

  const [item, setItem] = useState<ClothingItem | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [isLoading, user, router]);

  useEffect(() => {
    if (!token || Number.isNaN(itemId)) {
      return;
    }
    getClothingItem(token, itemId)
      .then(setItem)
      .catch((err) =>
        setError(err instanceof ApiError && err.status === 404 ? "Item not found." : "Couldn't load this item."),
      );
  }, [token, itemId]);

  if (isLoading || !user) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-md flex-1 flex-col gap-6 px-4 py-10">
      <button type="button" onClick={() => router.back()} className="self-start text-sm underline">
        ← Back
      </button>

      {error && <p className="text-sm text-red-600">{error}</p>}
      {!error && !item && <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading item…</p>}

      {item && (
        <div className="flex flex-col gap-3">
          <div className="relative aspect-square w-full overflow-hidden rounded-lg bg-zinc-100 dark:bg-zinc-900">
            <Image
              src={`${API_BASE_URL}${item.imageUrl}`}
              alt={item.name}
              fill
              sizes="(max-width: 640px) 100vw, 448px"
              className="object-cover"
            />
          </div>
          <div>
            <h1 className="text-xl font-semibold">{item.name}</h1>
            <p className="text-sm text-zinc-600 dark:text-zinc-400">
              {[item.brand, item.itemType, item.clothingSize, item.colour].filter(Boolean).join(" • ")}
            </p>
          </div>
          <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
            <dt className="text-zinc-600 dark:text-zinc-400">Condition</dt>
            <dd>{item.condition}</dd>
            <dt className="text-zinc-600 dark:text-zinc-400">Gender</dt>
            <dd>{item.gender}</dd>
          </dl>
          {item.description && <p className="text-sm text-zinc-600 dark:text-zinc-400">{item.description}</p>}
        </div>
      )}
    </div>
  );
}
