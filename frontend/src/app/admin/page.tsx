"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { apiFetch } from "@/lib/api/client";
import { Card } from "@/components/Card";
import { StatusBadge } from "@/components/StatusBadge";

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
      <p className="text-sm text-muted">
        Moderation views over users, clothing items, matches, swaps, messages, and reviews.
      </p>
      <Card className="p-4">
        <h2 className="mb-2 text-sm font-medium">Backend health</h2>
        {healthError && <p className="text-sm text-red-600">Couldn&apos;t reach /actuator/health.</p>}
        {!healthError && !health && <p className="text-sm text-muted">Checking…</p>}
        {health && (
          <p className="text-sm">
            Status: <StatusBadge label={health.status} tone={health.status === "UP" ? "positive" : "negative"} />
          </p>
        )}
      </Card>
    </div>
  );
}
