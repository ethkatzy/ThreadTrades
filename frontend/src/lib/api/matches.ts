import { apiFetch } from "./client";
import type { ClothingItem } from "./clothingItems";

export type Match = {
  id: number;
  otherUserId: number;
  otherUsername: string;
  otherUserName: string;
  otherUserProfilePictureUrl: string | null;
  myItem: ClothingItem;
  otherItem: ClothingItem;
  createdAt: string;
};

export function listMatches(token: string): Promise<Match[]> {
  return apiFetch<Match[]>("/api/matches", {}, token);
}
