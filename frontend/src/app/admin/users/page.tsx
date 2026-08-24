"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Pagination } from "@/components/Pagination";
import { listAdminUsers, type AdminUser, type PagedResponse } from "@/lib/api/admin";
import { StatusBadge, bannedTone, roleTone } from "@/components/StatusBadge";

export default function AdminUsersPage() {
  const { token } = useAuth();
  const [result, setResult] = useState<PagedResponse<AdminUser> | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    listAdminUsers(token, page, 20, search || undefined)
      .then((r) => {
        setResult(r);
        setError(null);
      })
      .catch(() => setError("Couldn't load users."));
  }, [token, page, search]);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-medium">Users</h2>
        <input
          type="text"
          placeholder="Search by email or username"
          value={search}
          onChange={(event) => {
            setPage(0);
            setSearch(event.target.value);
          }}
          className="rounded border border-border bg-transparent px-3 py-1.5 text-sm"
        />
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      {result && (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-muted">
                <th className="py-2 pr-4">Username</th>
                <th className="py-2 pr-4">Email</th>
                <th className="py-2 pr-4">Role</th>
                <th className="py-2 pr-4">Status</th>
                <th className="py-2">Joined</th>
              </tr>
            </thead>
            <tbody>
              {result.content.map((user) => (
                <tr key={user.id} className="border-b border-border/50">
                  <td className="py-2 pr-4">
                    <Link href={`/admin/users/${user.id}`} className="text-accent underline">
                      {user.username}
                    </Link>
                  </td>
                  <td className="py-2 pr-4">{user.email}</td>
                  <td className="py-2 pr-4">
                    <StatusBadge label={user.role} tone={roleTone(user.role)} />
                  </td>
                  <td className="py-2 pr-4">
                    <StatusBadge
                      label={user.banned ? "Banned" : "Active"}
                      tone={bannedTone(user.banned)}
                    />
                  </td>
                  <td className="py-2">{new Date(user.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
              {result.content.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-4 text-center text-muted">
                    No users found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {result && <Pagination page={result.page} totalPages={result.totalPages} onPageChange={setPage} />}
    </div>
  );
}
