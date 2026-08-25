const XSRF_COOKIE_NAME = "XSRF-TOKEN";

/**
 * Reads the CSRF token Spring Security's csrf.spa() writes as a (JS-readable,
 * non-httpOnly) cookie -- see backend SecurityConfig/CsrfCookieFilter. Echoed
 * back as the X-XSRF-TOKEN header on mutating requests in client.ts.
 */
export function getCsrfToken(): string | null {
  if (typeof document === "undefined") {
    return null;
  }
  const match = document.cookie.match(new RegExp(`(?:^|; )${XSRF_COOKIE_NAME}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}
