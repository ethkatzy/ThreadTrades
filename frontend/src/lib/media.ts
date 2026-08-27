const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * Resolves a stored image/profile-picture URL to something fetchable.
 * Both LocalDiskStorageService ("/uploads/...") and R2StorageService
 * ("/media/...", ROADMAP.md #1/#3) hand back a relative path on the API
 * origin; the http(s) branch below only still matters for rows persisted
 * before R2StorageService switched off returning R2's absolute public URL
 * directly (see the V8 migration backfilling those).
 */
export function mediaUrl(url: string): string {
  return /^https?:\/\//.test(url) ? url : `${API_BASE_URL}${url}`;
}
