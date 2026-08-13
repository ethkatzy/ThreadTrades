import { apiFetch } from "./client";

export type Message = {
  id: number;
  matchId: number;
  senderId: number;
  content: string;
  sentAt: string;
};

export function listMessages(token: string, matchId: number): Promise<Message[]> {
  return apiFetch<Message[]>(`/api/matches/${matchId}/messages`, {}, token);
}

export function sendMessage(token: string, matchId: number, content: string): Promise<Message> {
  return apiFetch<Message>(
    `/api/matches/${matchId}/messages`,
    { method: "POST", body: JSON.stringify({ content }) },
    token,
  );
}
