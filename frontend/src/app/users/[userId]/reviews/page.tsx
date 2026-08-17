"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/components/AuthProvider";
import { UserReviews } from "@/components/UserReviews";

export default function UserReviewsPage() {
  const { user, isLoading } = useAuth();
  const router = useRouter();
  const params = useParams<{ userId: string }>();
  const userId = Number(params.userId);

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [isLoading, user, router]);

  if (isLoading || !user) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-10">
      <button type="button" onClick={() => router.back()} className="self-start text-sm underline">
        ← Back
      </button>

      {Number.isNaN(userId) ? (
        <p className="text-sm text-red-600">Invalid user.</p>
      ) : (
        <UserReviews userId={userId} showIdentity />
      )}
    </div>
  );
}
