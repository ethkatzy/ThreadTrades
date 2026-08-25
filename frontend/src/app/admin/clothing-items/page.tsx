"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Pagination } from "@/components/Pagination";
import { listAdminClothingItems } from "@/lib/api/admin";
import type { PagedResponse } from "@/lib/api/admin";
import type { ClothingItem } from "@/lib/api/clothingItems";
import { StatusBadge, itemStatusTone } from "@/components/StatusBadge";

const STATUS_OPTIONS: Array<ClothingItem["status"] | ""> = ["", "AVAILABLE", "SWAPPED", "REMOVED"];

export default function AdminClothingItemsPage() {
  const [result, setResult] = useState<PagedResponse<ClothingItem> | null>(null);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<ClothingItem["status"] | "">("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listAdminClothingItems(page, 20, status || undefined)
      .then((r) => {
        setResult(r);
        setError(null);
      })
      .catch(() => setError("Couldn't load clothing items."));
  }, [page, status]);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-medium">Clothing items</h2>
        <select
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value as ClothingItem["status"] | "");
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

      {error && <p className="text-sm text-red-600">{error}</p>}

      {result && (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-muted">
                <th className="py-2 pr-4">Name</th>
                <th className="py-2 pr-4">Owner</th>
                <th className="py-2 pr-4">Status</th>
                <th className="py-2">Created</th>
              </tr>
            </thead>
            <tbody>
              {result.content.map((item) => (
                <tr key={item.id} className="border-b border-border/50">
                  <td className="py-2 pr-4">
                    <Link href={`/admin/clothing-items/${item.id}`} className="text-accent underline">
                      {item.name}
                    </Link>
                  </td>
                  <td className="py-2 pr-4">{item.ownerId}</td>
                  <td className="py-2 pr-4">
                    <StatusBadge label={item.status} tone={itemStatusTone(item.status)} />
                  </td>
                  <td className="py-2">{new Date(item.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
              {result.content.length === 0 && (
                <tr>
                  <td colSpan={4} className="py-4 text-center text-muted">
                    No items found.
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
