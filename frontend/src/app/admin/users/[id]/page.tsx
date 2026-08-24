"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { banUser, getAdminUser, unbanUser, type AdminUser } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";

export default function AdminUserDetailPage() {
  const { token } = useAuth();
  const params = useParams<{ id: string }>();
  const userId = Number(params.id);

  const [user, setUser] = useState<AdminUser | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!token) {
      return;
    }
    getAdminUser(token, userId)
      .then(setUser)
      .catch(() => setError("Couldn't load this user."));
  }, [token, userId]);

  async function handleToggleBan() {
    if (!token || !user) {
      return;
    }
    setIsSubmitting(true);
    setActionError(null);
    try {
      setUser(user.banned ? await unbanUser(token, userId) : await banUser(token, userId));
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
    return <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-medium">{user.username}</h2>
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
        <dt className="text-zinc-600 dark:text-zinc-400">Name</dt>
        <dd>{user.name}</dd>
        <dt className="text-zinc-600 dark:text-zinc-400">Email</dt>
        <dd>{user.email}</dd>
        <dt className="text-zinc-600 dark:text-zinc-400">Role</dt>
        <dd>{user.role}</dd>
        <dt className="text-zinc-600 dark:text-zinc-400">Status</dt>
        <dd>{user.banned ? "Banned" : "Active"}</dd>
        <dt className="text-zinc-600 dark:text-zinc-400">Joined</dt>
        <dd>{new Date(user.createdAt).toLocaleString()}</dd>
      </dl>

      {actionError && <p className="text-sm text-red-600">{actionError}</p>}
      <button
        type="button"
        disabled={isSubmitting}
        onClick={handleToggleBan}
        className="w-fit rounded border border-black/10 px-4 py-2 text-sm disabled:opacity-50 dark:border-white/20"
      >
        {user.banned ? "Unban user" : "Ban user"}
      </button>
    </div>
  );
}
