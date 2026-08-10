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
  },
};

export default nextConfig;
