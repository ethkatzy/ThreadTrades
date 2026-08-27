import type { NextConfig } from "next";

const apiUrl = new URL(process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080");

// Server-side only (not NEXT_PUBLIC_*): where THIS container reaches the backend to proxy
// /api/** through, as opposed to apiUrl above, which is the browser-facing URL used for
// images/WebSocket. In Docker Compose that's the backend container's service name
// (http://backend:8080); on Render, without private networking set up between the two
// services, it's the same public URL as apiUrl. Despite not being NEXT_PUBLIC_*, this
// still needs to be present at `next build` time, not just container start: rewrites()
// below is resolved once during the build and baked into .next/routes-manifest.json --
// `next start` does not re-invoke this function or re-read this var (see
// frontend/Dockerfile's ARG and ROADMAP.md #3).
const backendInternalUrl = process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // Proxies API calls through this server so the browser only ever talks to its own
  // origin -- see backendInternalUrl above and ROADMAP.md #3. Without this, tt_session/
  // XSRF-TOKEN are cross-site cookies: third-party cookie blocking can drop them
  // entirely, and even when it doesn't, the frontend's own JS can never read XSRF-TOKEN
  // via document.cookie to echo it back as the CSRF header, since it belongs to a
  // different site. Rewriting makes every Set-Cookie the backend sends look, to the
  // browser, like it came from this origin.
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendInternalUrl}/api/:path*` }];
  },
  images: {
    // "/uploads/**" (LocalDiskStorageService) and "/media/**"
    // (R2MediaController) are both served from the backend's own origin --
    // see StorageService.store()'s javadoc and ROADMAP.md #3.
    remotePatterns: ["/uploads/**", "/media/**"].map((pathname) => ({
      protocol: apiUrl.protocol.replace(":", "") as "http" | "https",
      hostname: apiUrl.hostname,
      port: apiUrl.port,
      pathname,
    })),
    // The backend runs on localhost/a private IP in dev (and in the current
    // Docker Compose setup -- see ROADMAP.md #2, no deployment target is
    // chosen yet). remotePatterns above already pins requests to exactly
    // that host/port/path, so allowing local IPs here doesn't widen what's
    // fetchable -- it only lifts Next's blanket private-IP block.
    dangerouslyAllowLocalIP: true,
    // next/image's optimizer fetches the source image SERVER-SIDE, i.e. from
    // inside the `frontend` container when running via Docker Compose --
    // NEXT_PUBLIC_API_URL=http://localhost:8080 means the browser, not that
    // container, so the server-side fetch 400s (nothing listens on the
    // frontend container's own loopback:8080). unoptimized skips the
    // server-side proxy entirely: the browser fetches the image directly
    // from the published host port instead, which works in both Docker
    // Compose and host-only dev. Trades away automatic resize/format
    // conversion; revisit (e.g. a container-internal NEXT_PUBLIC_ origin
    // just for image src, since the browser never dereferences that string
    // itself) if that optimization is ever worth the complexity.
    unoptimized: true,
  },
};

export default nextConfig;
