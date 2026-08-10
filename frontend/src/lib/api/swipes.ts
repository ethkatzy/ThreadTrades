import { apiFetch } from "./client";
import type { ClothingItem } from "./clothingItems";

export type SwipeDecision = "LIKE" | "DISLIKE";

export type SwipeResult = {
  id: number;
  offeredItemId: number;
  clothingItemId: number;
  decision: SwipeDecision;
  createdAt: string;
  matched: boolean;
  matchId: number | null;
};

export function getSwipeDeck(token: string, offeredItemId: number): Promise<ClothingItem[]> {
  return apiFetch<ClothingItem[]>(
    `/api/swipes/deck?offeredItemId=${offeredItemId}`,
    {},
    token,
  );
}

export function recordSwipe(
  token: string,
  offeredItemId: number,
  clothingItemId: number,
  decision: SwipeDecision,
): Promise<SwipeResult> {
  return apiFetch<SwipeResult>(
    "/api/swipes",
    { method: "POST", body: JSON.stringify({ offeredItemId, clothingItemId, decision }) },
    token,
  );
}
