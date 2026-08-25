import { apiFetch } from "./client";
import type { ClothingItem } from "./clothingItems";
import type { Message } from "./messages";
import type { Review } from "./reviews";
import type { SwapStatus } from "./swaps";

export type PagedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AdminUser = {
  id: number;
  appUserId: number;
  email: string;
  username: string;
  name: string;
  role: "USER" | "ADMIN";
  banned: boolean;
  createdAt: string;
};

export type AdminMatch = {
  id: number;
  userAId: number;
  userAUsername: string;
  itemA: ClothingItem;
  userBId: number;
  userBUsername: string;
  itemB: ClothingItem;
  createdAt: string;
  swapId: number | null;
  swapStatus: SwapStatus | null;
};

export type AdminSwap = {
  id: number;
  matchId: number;
  status: SwapStatus;
  acceptedByUserA: boolean;
  acceptedByUserB: boolean;
  createdAt: string;
  updatedAt: string;
};

function query(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") {
      search.set(key, String(value));
    }
  }
  const qs = search.toString();
  return qs ? `?${qs}` : "";
}

export function listAdminUsers(
  page = 0,
  size = 20,
  search?: string,
): Promise<PagedResponse<AdminUser>> {
  return apiFetch<PagedResponse<AdminUser>>(
    `/api/admin/users${query({ page, size, query: search })}`,
    {},
  );
}

export function getAdminUser(id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}`, {});
}

export function banUser(id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/ban`, { method: "POST" });
}

export function unbanUser(id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/unban`, { method: "POST" });
}

export function promoteUser(id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/promote`, { method: "POST" });
}

export function demoteUser(id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/demote`, { method: "POST" });
}

export function listAdminClothingItems(
  page = 0,
  size = 20,
  status?: ClothingItem["status"],
): Promise<PagedResponse<ClothingItem>> {
  return apiFetch<PagedResponse<ClothingItem>>(
    `/api/admin/clothing-items${query({ page, size, status })}`,
    {},
  );
}

export function getAdminClothingItem(id: number): Promise<ClothingItem> {
  return apiFetch<ClothingItem>(`/api/admin/clothing-items/${id}`, {});
}

export function removeClothingItem(id: number): Promise<ClothingItem> {
  return apiFetch<ClothingItem>(`/api/admin/clothing-items/${id}/remove`, { method: "POST" });
}

export function listAdminMatches(page = 0, size = 20): Promise<PagedResponse<AdminMatch>> {
  return apiFetch<PagedResponse<AdminMatch>>(`/api/admin/matches${query({ page, size })}`, {});
}

export function getAdminMatch(id: number): Promise<AdminMatch> {
  return apiFetch<AdminMatch>(`/api/admin/matches/${id}`, {});
}

export function listAdminSwaps(
  page = 0,
  size = 20,
  status?: SwapStatus,
): Promise<PagedResponse<AdminSwap>> {
  return apiFetch<PagedResponse<AdminSwap>>(`/api/admin/swaps${query({ page, size, status })}`, {});
}

export function getAdminSwap(id: number): Promise<AdminSwap> {
  return apiFetch<AdminSwap>(`/api/admin/swaps/${id}`, {});
}

export function forceResolveSwap(id: number, status: "ACCEPTED" | "REJECTED"): Promise<AdminSwap> {
  return apiFetch<AdminSwap>(
    `/api/admin/swaps/${id}/force-resolve`,
    { method: "POST", body: JSON.stringify({ status }) },
  );
}

export function listAdminMessages(
  page = 0,
  size = 20,
  matchId?: number,
): Promise<PagedResponse<Message>> {
  return apiFetch<PagedResponse<Message>>(`/api/admin/messages${query({ page, size, matchId })}`, {});
}

export function deleteAdminMessage(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/messages/${id}`, { method: "DELETE" });
}

export function listAdminReviews(page = 0, size = 20): Promise<PagedResponse<Review>> {
  return apiFetch<PagedResponse<Review>>(`/api/admin/reviews${query({ page, size })}`, {});
}

export function deleteAdminReview(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/reviews/${id}`, { method: "DELETE" });
}
