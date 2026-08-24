"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { apiFetch } from "@/lib/api/client";

type HealthStatus = { status: string };

export default function AdminDashboardPage() {
  const { token } = useAuth();
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [healthError, setHealthError] = useState(false);

  useEffect(() => {
    if (!token) {
      return;
    }
    apiFetch<HealthStatus>("/actuator/health", {}, token)
      .then(setHealth)
      .catch(() => setHealthError(true));
  }, [token]);

  return (
    <div className="flex flex-col gap-6">
      <p className="text-sm text-zinc-600 dark:text-zinc-400">
        Moderation views over users, clothing items, matches, swaps, messages, and reviews.
      </p>
      <div className="rounded-lg border border-black/10 p-4 dark:border-white/20">
        <h2 className="mb-2 text-sm font-medium">Backend health</h2>
        {healthError && <p className="text-sm text-red-600">Couldn&apos;t reach /actuator/health.</p>}
        {!healthError && !health && <p className="text-sm text-zinc-600 dark:text-zinc-400">Checking…</p>}
        {health && (
          <p className="text-sm">
            Status:{" "}
            <span className={health.status === "UP" ? "text-green-600" : "text-red-600"}>{health.status}</span>
          </p>
        )}
      </div>
    </div>
  );
}
