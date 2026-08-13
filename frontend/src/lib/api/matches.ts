import type { UserProfile } from "./auth";
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

export function getMatchOtherUser(token: string, matchId: number): Promise<UserProfile> {
  return apiFetch<UserProfile>(`/api/matches/${matchId}/other-user`, {}, token);
}
