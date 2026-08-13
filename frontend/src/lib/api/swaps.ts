import { apiFetch } from "./client";

export type SwapStatus = "PENDING" | "ACCEPTED" | "REJECTED";

export type Swap = {
  id: number;
  matchId: number;
  status: SwapStatus;
  iAccepted: boolean;
  otherAccepted: boolean;
  updatedAt: string;
};

export function getSwap(token: string, matchId: number): Promise<Swap> {
  return apiFetch<Swap>(`/api/matches/${matchId}/swap`, {}, token);
}

export function acceptSwap(token: string, matchId: number): Promise<Swap> {
  return apiFetch<Swap>(`/api/matches/${matchId}/swap/accept`, { method: "PATCH" }, token);
}

export function rejectSwap(token: string, matchId: number): Promise<Swap> {
  return apiFetch<Swap>(`/api/matches/${matchId}/swap/reject`, { method: "PATCH" }, token);
}
