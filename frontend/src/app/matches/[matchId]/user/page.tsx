"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { UserReviews } from "@/components/UserReviews";
import type { UserProfile } from "@/lib/api/auth";
import type { ClothingItem } from "@/lib/api/clothingItems";
import { ApiError } from "@/lib/api/client";
import { getMatchOtherUser } from "@/lib/api/matches";
import { listOtherUserSwapItems } from "@/lib/api/swaps";
import { mediaUrl } from "@/lib/media";

const tabClassName = (active: boolean) =>
  `pb-2 text-sm font-medium ${active ? "border-b-2 border-accent text-accent" : "text-muted"}`;

export default function MatchedUserProfilePage() {
  const { user, isLoading } = useAuth();
  const router = useRouter();
  const params = useParams<{ matchId: string }>();
  const matchId = Number(params.matchId);

  const [activeTab, setActiveTab] = useState<"profile" | "items" | "reviews">("profile");
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [items, setItems] = useState<ClothingItem[] | null>(null);
  const [itemsError, setItemsError] = useState<string | null>(null);

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
    listOtherUserSwapItems(matchId)
      .then(setItems)
      .catch((err) =>
        setItemsError(err instanceof ApiError ? err.message : "Couldn't load this user's items."),
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
            <button type="button" onClick={() => setActiveTab("items")} className={tabClassName(activeTab === "items")}>
              Items
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

          {activeTab === "items" && (
            <div className="flex flex-col gap-3">
              <p className="text-sm text-muted">
                {profile.name}&apos;s other available items -- things you could add to this trade.
              </p>
              {itemsError && <p className="text-sm text-red-600">{itemsError}</p>}
              {items === null && !itemsError && <p className="text-sm text-muted">Loading items…</p>}
              {items && items.length === 0 && (
                <p className="text-sm text-muted">No other available items right now.</p>
              )}
              {items && items.length > 0 && (
                <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                  {items.map((item) => (
                    <li key={item.id}>
                      <Link
                        href={`/clothing-items/${item.id}`}
                        className="flex flex-col gap-1 rounded border border-border p-2 text-left"
                      >
                        <div className="relative aspect-square w-full overflow-hidden rounded bg-surface">
                          <Image
                            src={mediaUrl(item.imageUrl)}
                            alt={item.name}
                            fill
                            sizes="(max-width: 640px) 50vw, 33vw"
                            className="object-cover"
                          />
                        </div>
                        <span className="text-sm">{item.name}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}

          {activeTab === "reviews" && <UserReviews userId={profile.id} />}
        </>
      )}
    </div>
  );
}
