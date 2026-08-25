import { getCsrfToken } from "./csrf";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function readErrorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => "");
  if (!text) {
    return response.statusText;
  }
  try {
    const body = JSON.parse(text) as { message?: string };
    return body.message ?? text;
  } catch {
    return text;
  }
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type") && options.body && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  // The session lives in an httpOnly cookie (never touched by JS -- see ROADMAP.md #3), so
  // credentials: "include" is what actually authenticates every request now, not a header.
  // Mutating requests additionally need the CSRF double-submit header: the backend's
  // csrf.spa() compares this against the (JS-readable) XSRF-TOKEN cookie it also set.
  const method = (options.method ?? "GET").toUpperCase();
  if (method !== "GET" && method !== "HEAD") {
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers.set("X-XSRF-TOKEN", csrfToken);
    }
  }

  // Relative, not an absolute backend URL: next.config.ts's rewrites() proxies /api/**
  // server-side to the real backend, so the browser only ever talks to its own origin.
  // That's not just cosmetic -- it's what makes tt_session/XSRF-TOKEN work at all on
  // Render, where frontend/backend are separate *.onrender.com sites (see ROADMAP.md #3):
  // a same-site cookie is readable via document.cookie and isn't subject to third-party
  // cookie blocking, neither of which any cookie flag combination can fix cross-site.
  const response = await fetch(path, {
    ...options,
    headers,
    credentials: "include",
  });

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response));
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
