import { Client, type IMessage } from "@stomp/stompjs";
import type { Message } from "@/lib/api/messages";
import type { Swap } from "@/lib/api/swaps";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

function wsUrl(): string {
  return API_BASE_URL.replace(/^http/, "ws") + "/ws";
}

type ConnectMatchSocketOptions = {
  token: string;
  matchId: number;
  onMessage: (message: Message) => void;
  onSwapUpdate?: (swap: Swap) => void;
  onConnectedChange?: (connected: boolean) => void;
};

/**
 * Opens a STOMP connection and subscribes to one match's live thread
 * (backend: MatchTopics.destination) and its swap-status updates (backend:
 * SwapTopics.destination). The JWT travels as a STOMP CONNECT header, not the
 * WebSocket handshake itself, since browsers can't attach custom headers to
 * that handshake -- see StompAuthChannelInterceptor.
 * Caller owns the returned client's lifecycle (call deactivate() on unmount).
 */
export function connectMatchSocket(options: ConnectMatchSocketOptions): Client {
  const client = new Client({
    brokerURL: wsUrl(),
    connectHeaders: { Authorization: `Bearer ${options.token}` },
    reconnectDelay: 5000,
    onConnect: () => {
      options.onConnectedChange?.(true);
      client.subscribe(`/topic/matches/${options.matchId}`, (frame: IMessage) => {
        options.onMessage(JSON.parse(frame.body) as Message);
      });
      client.subscribe(`/topic/matches/${options.matchId}/swap`, (frame: IMessage) => {
        options.onSwapUpdate?.(JSON.parse(frame.body) as Swap);
      });
    },
    onWebSocketClose: () => options.onConnectedChange?.(false),
  });
  client.activate();
  return client;
}
