import type { UserProfile } from "./auth";
import { apiFetch } from "./client";
import type { ClothingItem } from "./clothingItems";

export type Match = {
  id: number;
  otherUserId: number;
  otherUsername: string;
  otherUserName: string;
  otherUserProfilePictureUrl: string | null;
  otherUserAverageRating: number | null;
  otherUserReviewCount: number;
  myItem: ClothingItem;
  otherItem: ClothingItem;
  createdAt: string;
};

export function listMatches(): Promise<Match[]> {
  return apiFetch<Match[]>("/api/matches", {});
}

export function getMatchOtherUser(matchId: number): Promise<UserProfile> {
  return apiFetch<UserProfile>(`/api/matches/${matchId}/other-user`, {});
}
