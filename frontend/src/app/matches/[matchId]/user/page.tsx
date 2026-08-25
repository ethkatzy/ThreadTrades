"use client";

import Image from "next/image";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { UserReviews } from "@/components/UserReviews";
import type { UserProfile } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { getMatchOtherUser } from "@/lib/api/matches";
import { mediaUrl } from "@/lib/media";

const tabClassName = (active: boolean) =>
  `pb-2 text-sm font-medium ${active ? "border-b-2 border-accent text-accent" : "text-muted"}`;

export default function MatchedUserProfilePage() {
  const { user, isLoading } = useAuth();
  const router = useRouter();
  const params = useParams<{ matchId: string }>();
  const matchId = Number(params.matchId);

  const [activeTab, setActiveTab] = useState<"profile" | "reviews">("profile");
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [isLoading, user, router]);

  useEffect(() => {
    if (!user || Number.isNaN(matchId)) {
      return;
    }
    getMatchOtherUser(matchId)
      .then(setProfile)
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : "Couldn't load this user's profile."),
      );
  }, [user, matchId]);

  if (isLoading || !user) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-muted">Loading…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-10">
      <button type="button" onClick={() => router.back()} className="self-start text-sm text-accent underline">
        ← Back
      </button>

      {error && <p className="text-sm text-red-600">{error}</p>}
      {!profile && !error && <p className="text-sm text-muted">Loading profile…</p>}

      {profile && (
        <>
          <div className="flex gap-4 border-b border-border">
            <button type="button" onClick={() => setActiveTab("profile")} className={tabClassName(activeTab === "profile")}>
              Profile
            </button>
            <button type="button" onClick={() => setActiveTab("reviews")} className={tabClassName(activeTab === "reviews")}>
              Reviews
            </button>
          </div>

          {activeTab === "profile" && (
            <div className="flex flex-col items-center gap-4 text-center">
              <div className="relative h-24 w-24 overflow-hidden rounded-full bg-surface">
                {profile.profilePictureUrl && (
                  <Image
                    src={mediaUrl(profile.profilePictureUrl)}
                    alt={profile.name}
                    fill
                    sizes="96px"
                    className="object-cover"
                  />
                )}
              </div>
              <div>
                <h1 className="text-xl font-semibold">{profile.name}</h1>
                <p className="text-sm text-muted">@{profile.username}</p>
              </div>
              {profile.bio && <p className="max-w-sm text-sm">{profile.bio}</p>}
            </div>
          )}

          {activeTab === "reviews" && <UserReviews userId={profile.id} />}
        </>
      )}
    </div>
  );
}
