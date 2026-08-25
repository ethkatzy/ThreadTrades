"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { banUser, demoteUser, getAdminUser, promoteUser, unbanUser, type AdminUser } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Button } from "@/components/Button";
import { StatusBadge, bannedTone, roleTone } from "@/components/StatusBadge";

export default function AdminUserDetailPage() {
  const params = useParams<{ id: string }>();
  const userId = Number(params.id);

  const [user, setUser] = useState<AdminUser | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    getAdminUser(userId)
      .then(setUser)
      .catch(() => setError("Couldn't load this user."));
  }, [userId]);

  async function handleToggleBan() {
    if (!user) {
      return;
    }
    setIsSubmitting(true);
    setActionError(null);
    try {
      setUser(user.banned ? await unbanUser(userId) : await banUser(userId));
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't update this user.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleToggleRole() {
    if (!user) {
      return;
    }
    setIsSubmitting(true);
    setActionError(null);
    try {
      setUser(user.role === "ADMIN" ? await demoteUser(userId) : await promoteUser(userId));
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't update this user.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (error) {
    return <p className="text-sm text-red-600">{error}</p>;
  }
  if (!user) {
    return <p className="text-sm text-muted">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-medium">{user.username}</h2>
      <dl className="grid grid-cols-[auto_1fr] items-center gap-x-4 gap-y-1 text-sm">
        <dt className="text-muted">Name</dt>
        <dd>{user.name}</dd>
        <dt className="text-muted">Email</dt>
        <dd>{user.email}</dd>
        <dt className="text-muted">Role</dt>
        <dd>
          <StatusBadge label={user.role} tone={roleTone(user.role)} />
        </dd>
        <dt className="text-muted">Status</dt>
        <dd>
          <StatusBadge label={user.banned ? "Banned" : "Active"} tone={bannedTone(user.banned)} />
        </dd>
        <dt className="text-muted">Joined</dt>
        <dd>{new Date(user.createdAt).toLocaleString()}</dd>
      </dl>

      {actionError && <p className="text-sm text-red-600">{actionError}</p>}
      <div className="flex gap-3">
        <Button
          type="button"
          variant="secondary"
          disabled={isSubmitting}
          onClick={handleToggleBan}
          className="w-fit"
        >
          {user.banned ? "Unban user" : "Ban user"}
        </Button>
        <Button
          type="button"
          variant="secondary"
          disabled={isSubmitting}
          onClick={handleToggleRole}
          className="w-fit"
        >
          {user.role === "ADMIN" ? "Remove admin" : "Make admin"}
        </Button>
      </div>
    </div>
  );
}
