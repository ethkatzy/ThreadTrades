"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ApiError } from "@/lib/api/client";
import type { ClothingItem } from "@/lib/api/clothingItems";
import {
  addExtraItem,
  clearCashTerm,
  listOtherUserSwapItems,
  removeExtraItem,
  setCashTerm,
  type Swap,
  type SwapExtraItem,
} from "@/lib/api/swaps";
import { Button } from "@/components/Button";

type Props = {
  matchId: number;
  swap: Swap;
  myItems: ClothingItem[];
  baseItemId: number;
  otherUserName: string;
  onUpdate: (swap: Swap) => void;
};

function formatPence(pence: number): string {
  return `£${(pence / 100).toFixed(2)}`;
}

function ItemLink({ extra }: { extra: SwapExtraItem }) {
  return (
    <Link href={`/clothing-items/${extra.item.id}`} className="text-accent underline">
      {extra.item.name}
    </Link>
  );
}

// <select> renders its dropdown popup as a separate browser-native layer that doesn't
// inherit the page's dark background, so without an explicit background/text color here
// the popup falls back to light-on-white and only the highlighted option stays readable
// (same fix as clothing-items/upload/page.tsx's selectClassName).
const selectClassName = "rounded border border-border bg-background px-2 py-1 text-xs text-foreground";

export function SwapNegotiationPanel({ matchId, swap, myItems, baseItemId, otherUserName, onUpdate }: Props) {
  const [otherItems, setOtherItems] = useState<ClothingItem[]>([]);
  const [selectedMyItemId, setSelectedMyItemId] = useState("");
  const [selectedTheirItemId, setSelectedTheirItemId] = useState("");
  const [isSubmittingItem, setIsSubmittingItem] = useState(false);
  const [itemError, setItemError] = useState<string | null>(null);

  const [cashAmount, setCashAmount] = useState("");
  const [iOwe, setIOwe] = useState(true);
  const [isSubmittingCash, setIsSubmittingCash] = useState(false);
  const [cashError, setCashError] = useState<string | null>(null);

  useEffect(() => {
    listOtherUserSwapItems(matchId)
      .then(setOtherItems)
      .catch(() => setItemError((prev) => prev ?? `Couldn't load ${otherUserName}'s items.`));
  }, [matchId, otherUserName]);

  const myAddedIds = new Set(swap.myExtraItems.map((extra) => extra.item.id));
  const otherAddedIds = new Set(swap.otherExtraItems.map((extra) => extra.item.id));
  const selectableMyItems = myItems.filter(
    (item) => item.id !== baseItemId && item.status === "AVAILABLE" && !myAddedIds.has(item.id),
  );
  const selectableTheirItems = otherItems.filter((item) => !otherAddedIds.has(item.id));

  async function handleAddItem(clothingItemId: number, reset: () => void) {
    if (!clothingItemId || isSubmittingItem) {
      return;
    }
    setIsSubmittingItem(true);
    setItemError(null);
    try {
      onUpdate(await addExtraItem(matchId, clothingItemId));
      reset();
    } catch (err) {
      setItemError(err instanceof ApiError ? err.message : "Couldn't add that item.");
    } finally {
      setIsSubmittingItem(false);
    }
  }

  async function handleRemoveItem(clothingItemId: number) {
    if (isSubmittingItem) {
      return;
    }
    setIsSubmittingItem(true);
    setItemError(null);
    try {
      onUpdate(await removeExtraItem(matchId, clothingItemId));
    } catch (err) {
      setItemError(err instanceof ApiError ? err.message : "Couldn't remove that item.");
    } finally {
      setIsSubmittingItem(false);
    }
  }

  async function handleSetCash() {
    const pounds = Number(cashAmount);
    if (!pounds || pounds <= 0 || isSubmittingCash) {
      return;
    }
    setIsSubmittingCash(true);
    setCashError(null);
    try {
      onUpdate(await setCashTerm(matchId, Math.round(pounds * 100), iOwe));
      setCashAmount("");
    } catch (err) {
      setCashError(err instanceof ApiError ? err.message : "Couldn't set that amount.");
    } finally {
      setIsSubmittingCash(false);
    }
  }

  async function handleClearCash() {
    if (isSubmittingCash) {
      return;
    }
    setIsSubmittingCash(true);
    setCashError(null);
    try {
      onUpdate(await clearCashTerm(matchId));
    } catch (err) {
      setCashError(err instanceof ApiError ? err.message : "Couldn't clear that amount.");
    } finally {
      setIsSubmittingCash(false);
    }
  }

  return (
    <div className="flex flex-col gap-3 border-t border-border pt-3">
      <p className="text-xs font-medium text-muted">Not quite even? Add another item, request one, or a cash top-up.</p>

      {(swap.myExtraItems.length > 0 || swap.otherExtraItems.length > 0) && (
        <div className="flex flex-col gap-1.5">
          {swap.myExtraItems.map((extra) => (
            <div key={extra.item.id} className="flex items-center justify-between gap-2 text-xs">
              <span>
                {extra.proposedByMe ? (
                  <>
                    You&apos;re also adding <ItemLink extra={extra} />
                  </>
                ) : (
                  <>
                    {otherUserName} is requesting <ItemLink extra={extra} /> from you
                  </>
                )}
              </span>
              <button
                type="button"
                onClick={() => handleRemoveItem(extra.item.id)}
                disabled={isSubmittingItem}
                className="text-muted hover:text-foreground"
                aria-label={`Remove ${extra.item.name}`}
              >
                ×
              </button>
            </div>
          ))}
          {swap.otherExtraItems.map((extra) => (
            <div key={extra.item.id} className="flex items-center justify-between gap-2 text-xs">
              <span>
                {extra.proposedByMe ? (
                  <>
                    You&apos;re requesting <ItemLink extra={extra} /> from {otherUserName}
                  </>
                ) : (
                  <>
                    {otherUserName} is also adding <ItemLink extra={extra} />
                  </>
                )}
              </span>
              <button
                type="button"
                onClick={() => handleRemoveItem(extra.item.id)}
                disabled={isSubmittingItem}
                className="text-muted hover:text-foreground"
                aria-label={`Remove ${extra.item.name}`}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      {swap.cashAmountPence !== null && (
        <div className="flex items-center justify-between gap-2 text-xs">
          <span>
            {swap.iOweCash ? "You're" : "They're"} adding {formatPence(swap.cashAmountPence)} cash
          </span>
          <button
            type="button"
            onClick={handleClearCash}
            disabled={isSubmittingCash}
            className="text-muted hover:text-foreground"
            aria-label="Clear cash amount"
          >
            ×
          </button>
        </div>
      )}
      {cashError && <p className="text-xs text-red-600">{cashError}</p>}

      {selectableMyItems.length > 0 ? (
        <div className="flex items-center gap-2">
          <select
            value={selectedMyItemId}
            onChange={(event) => setSelectedMyItemId(event.target.value)}
            className={`flex-1 ${selectClassName}`}
          >
            <option value="">Add one of your items…</option>
            {selectableMyItems.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
          <Button
            type="button"
            size="sm"
            onClick={() => handleAddItem(Number(selectedMyItemId), () => setSelectedMyItemId(""))}
            disabled={!selectedMyItemId || isSubmittingItem}
          >
            Add
          </Button>
        </div>
      ) : (
        <p className="text-xs text-muted">
          No other available items to add —{" "}
          <Link href="/clothing-items/upload" className="text-accent underline">
            upload one
          </Link>{" "}
          to offer it here.
        </p>
      )}

      {selectableTheirItems.length > 0 && (
        <div className="flex items-center gap-2">
          <select
            value={selectedTheirItemId}
            onChange={(event) => setSelectedTheirItemId(event.target.value)}
            className={`flex-1 ${selectClassName}`}
          >
            <option value="">Request one of {otherUserName}&apos;s items…</option>
            {selectableTheirItems.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
          <Button
            type="button"
            size="sm"
            variant="secondary"
            onClick={() => handleAddItem(Number(selectedTheirItemId), () => setSelectedTheirItemId(""))}
            disabled={!selectedTheirItemId || isSubmittingItem}
          >
            Request
          </Button>
        </div>
      )}
      {itemError && <p className="text-xs text-red-600">{itemError}</p>}

      <div className="flex items-center gap-2">
        <span className="text-xs">£</span>
        <input
          type="number"
          min="0.01"
          step="0.01"
          value={cashAmount}
          onChange={(event) => setCashAmount(event.target.value)}
          placeholder="0.00"
          className="w-20 rounded border border-border px-2 py-1 text-xs"
        />
        <select
          value={iOwe ? "me" : "them"}
          onChange={(event) => setIOwe(event.target.value === "me")}
          className={selectClassName}
        >
          <option value="me">I&apos;ll add this</option>
          <option value="them">They should add this</option>
        </select>
        <Button
          type="button"
          size="sm"
          variant="secondary"
          onClick={handleSetCash}
          disabled={!cashAmount || isSubmittingCash}
        >
          Set
        </Button>
      </div>
    </div>
  );
}
