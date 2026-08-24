import type { NextConfig } from "next";

const apiUrl = new URL(process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080");

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: apiUrl.protocol.replace(":", "") as "http" | "https",
        hostname: apiUrl.hostname,
        port: apiUrl.port,
        pathname: "/uploads/**",
      },
    ],
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
