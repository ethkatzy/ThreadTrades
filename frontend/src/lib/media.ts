const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * Resolves a stored image/profile-picture URL to something fetchable.
 * LocalDiskStorageService hands back a relative "/uploads/..." path that
 * needs the API origin prefixed; R2StorageService (ROADMAP.md #1) hands back
 * an already-absolute URL that must be used as-is -- prefixing it again would
 * produce garbage like "http://api.example.com/https://pub-xyz.r2.dev/...".
 */
export function mediaUrl(url: string): string {
  return /^https?:\/\//.test(url) ? url : `${API_BASE_URL}${url}`;
}
