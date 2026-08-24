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
  token: string,
  page = 0,
  size = 20,
  search?: string,
): Promise<PagedResponse<AdminUser>> {
  return apiFetch<PagedResponse<AdminUser>>(
    `/api/admin/users${query({ page, size, query: search })}`,
    {},
    token,
  );
}

export function getAdminUser(token: string, id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}`, {}, token);
}

export function banUser(token: string, id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/ban`, { method: "POST" }, token);
}

export function unbanUser(token: string, id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/unban`, { method: "POST" }, token);
}

export function promoteUser(token: string, id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/promote`, { method: "POST" }, token);
}

export function demoteUser(token: string, id: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/demote`, { method: "POST" }, token);
}

export function listAdminClothingItems(
  token: string,
  page = 0,
  size = 20,
  status?: ClothingItem["status"],
): Promise<PagedResponse<ClothingItem>> {
  return apiFetch<PagedResponse<ClothingItem>>(
    `/api/admin/clothing-items${query({ page, size, status })}`,
    {},
    token,
  );
}

export function getAdminClothingItem(token: string, id: number): Promise<ClothingItem> {
  return apiFetch<ClothingItem>(`/api/admin/clothing-items/${id}`, {}, token);
}

export function removeClothingItem(token: string, id: number): Promise<ClothingItem> {
  return apiFetch<ClothingItem>(`/api/admin/clothing-items/${id}/remove`, { method: "POST" }, token);
}

export function listAdminMatches(token: string, page = 0, size = 20): Promise<PagedResponse<AdminMatch>> {
  return apiFetch<PagedResponse<AdminMatch>>(`/api/admin/matches${query({ page, size })}`, {}, token);
}

export function getAdminMatch(token: string, id: number): Promise<AdminMatch> {
  return apiFetch<AdminMatch>(`/api/admin/matches/${id}`, {}, token);
}

export function listAdminSwaps(
  token: string,
  page = 0,
  size = 20,
  status?: SwapStatus,
): Promise<PagedResponse<AdminSwap>> {
  return apiFetch<PagedResponse<AdminSwap>>(`/api/admin/swaps${query({ page, size, status })}`, {}, token);
}

export function getAdminSwap(token: string, id: number): Promise<AdminSwap> {
  return apiFetch<AdminSwap>(`/api/admin/swaps/${id}`, {}, token);
}

export function forceResolveSwap(token: string, id: number, status: "ACCEPTED" | "REJECTED"): Promise<AdminSwap> {
  return apiFetch<AdminSwap>(
    `/api/admin/swaps/${id}/force-resolve`,
    { method: "POST", body: JSON.stringify({ status }) },
    token,
  );
}

export function listAdminMessages(
  token: string,
  page = 0,
  size = 20,
  matchId?: number,
): Promise<PagedResponse<Message>> {
  return apiFetch<PagedResponse<Message>>(`/api/admin/messages${query({ page, size, matchId })}`, {}, token);
}

export function deleteAdminMessage(token: string, id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/messages/${id}`, { method: "DELETE" }, token);
}

export function listAdminReviews(token: string, page = 0, size = 20): Promise<PagedResponse<Review>> {
  return apiFetch<PagedResponse<Review>>(`/api/admin/reviews${query({ page, size })}`, {}, token);
}

export function deleteAdminReview(token: string, id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/reviews/${id}`, { method: "DELETE" }, token);
}
