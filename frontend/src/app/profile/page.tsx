"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { UserReviews } from "@/components/UserReviews";
import { ApiError } from "@/lib/api/client";
import { listSwapHistory, type SwapExtraItem, type SwapHistoryEntry } from "@/lib/api/swaps";
import { Button } from "@/components/Button";
import { Card } from "@/components/Card";
import { mediaUrl } from "@/lib/media";

const inputClassName = "rounded border border-border px-3 py-2";
const tabClassName = (active: boolean) =>
  `pb-2 text-sm font-medium ${active ? "border-b-2 border-accent text-accent" : "text-muted"}`;

function formatPence(pence: number): string {
  return `£${(pence / 100).toFixed(2)}`;
}

function ExtraItemLinks({ items }: { items: SwapExtraItem[] }) {
  return (
    <>
      {items.map((extra, index) => (
        <span key={extra.item.id}>
          {index > 0 && ", "}
          <Link href={`/clothing-items/${extra.item.id}`} className="text-accent underline">
            {extra.item.name}
          </Link>
        </span>
      ))}
    </>
  );
}

export default function ProfilePage() {
  const { user, isLoading, updateProfile } = useAuth();
  const router = useRouter();

  const [activeTab, setActiveTab] = useState<"profile" | "history" | "reviews">("profile");
  const [isEditing, setIsEditing] = useState(false);
  const [name, setName] = useState("");
  const [bio, setBio] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [history, setHistory] = useState<SwapHistoryEntry[] | null>(null);
  const [historyError, setHistoryError] = useState<string | null>(null);

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [isLoading, user, router]);

  useEffect(() => {
    if (user) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- hydrating the edit form once the async-loaded user arrives, not a derived render value
      setName(user.name);
      setBio(user.bio ?? "");
    }
  }, [user]);

  // Object URLs are local-only and can't go through next/image's optimizer,
  // so the avatar below uses a plain <img> for both the preview and the
  // persisted picture rather than switching elements depending on state.
  useEffect(() => {
    if (!image) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- subscribing to the browser's object-URL lifecycle, not a derived render value
      setPreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(image);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [image]);

  useEffect(() => {
    if (!user || activeTab !== "history" || history !== null) {
      return;
    }
    listSwapHistory()
      .then(setHistory)
      .catch(() => setHistoryError("Couldn't load your swap history."));
  }, [user, activeTab, history]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSaved(false);
    setIsSubmitting(true);
    try {
      const formData = new FormData();
      formData.append("name", name);
      formData.append("bio", bio);
      if (image) {
        formData.append("image", image);
      }
      await updateProfile(formData);
      setImage(null);
      setSaved(true);
      setIsEditing(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleCancel() {
    if (user) {
      setName(user.name);
      setBio(user.bio ?? "");
    }
    setImage(null);
    setError(null);
    setIsEditing(false);
  }

  if (isLoading || !user) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-muted">Loading…</p>
      </div>
    );
  }

  const avatarSrc = previewUrl ?? (user.profilePictureUrl ? mediaUrl(user.profilePictureUrl) : null);

  if (!isEditing) {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-10">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold">Your profile</h1>
          {activeTab === "profile" && (
            <Button type="button" onClick={() => setIsEditing(true)}>
              Edit
            </Button>
          )}
        </div>

        <div className="flex gap-4 border-b border-border">
          <button type="button" onClick={() => setActiveTab("profile")} className={tabClassName(activeTab === "profile")}>
            Profile
          </button>
          <button type="button" onClick={() => setActiveTab("history")} className={tabClassName(activeTab === "history")}>
            Swap history
          </button>
          <button type="button" onClick={() => setActiveTab("reviews")} className={tabClassName(activeTab === "reviews")}>
            Reviews
          </button>
        </div>

        {activeTab === "reviews" && <UserReviews userId={user.id} />}

        {activeTab === "profile" && (
          <>
            {saved && <p className="text-sm text-accent">Saved.</p>}

            <div className="flex flex-col items-center gap-4 text-center">
              <div className="relative h-24 w-24 overflow-hidden rounded-full bg-surface">
                {user.profilePictureUrl && (
                  <Image
                    src={mediaUrl(user.profilePictureUrl)}
                    alt={user.name}
                    fill
                    sizes="96px"
                    className="object-cover"
                  />
                )}
              </div>
              <div>
                <h2 className="text-xl font-semibold">{user.name}</h2>
                <p className="text-sm text-muted">@{user.username}</p>
              </div>
              {user.bio && <p className="max-w-sm text-sm">{user.bio}</p>}
            </div>
          </>
        )}

        {activeTab === "history" && (
          <div className="flex flex-col gap-4">
            {historyError && <p className="text-sm text-red-600">{historyError}</p>}
            {history === null && !historyError && (
              <p className="text-sm text-muted">Loading swap history…</p>
            )}
            {history && history.length === 0 && (
              <p className="text-sm text-muted">No completed swaps yet.</p>
            )}
            {history && history.length > 0 && (
              <ul className="flex flex-col gap-4">
                {history.map((entry) => (
                  <Card as="li" key={entry.matchId} className="flex items-center gap-4">
                    <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded border border-border bg-surface">
                      <Image
                        src={mediaUrl(entry.myItem.imageUrl)}
                        alt={entry.myItem.name}
                        fill
                        sizes="64px"
                        className="object-cover"
                      />
                    </div>
                    <div className="min-w-0 flex-1 text-sm">
                      <p>
                        Swapped your <span className="font-medium">{entry.myItem.name}</span> for{" "}
                        <span className="font-medium">{entry.otherItem.name}</span> with{" "}
                        <Link href={`/matches/${entry.matchId}/user`} className="text-accent underline">
                          {entry.otherUserName}
                        </Link>
                      </p>
                      {(entry.myExtraItems.length > 0 ||
                        entry.otherExtraItems.length > 0 ||
                        entry.cashAmountPence !== null) && (
                        <p className="text-xs text-muted">
                          {entry.myExtraItems.length > 0 && (
                            <>
                              You also added <ExtraItemLinks items={entry.myExtraItems} />.{" "}
                            </>
                          )}
                          {entry.otherExtraItems.length > 0 && (
                            <>
                              {entry.otherUserName} also added <ExtraItemLinks items={entry.otherExtraItems} />.{" "}
                            </>
                          )}
                          {entry.cashAmountPence !== null && (
                            <>
                              {entry.iOweCash ? "You" : entry.otherUserName} added{" "}
                              {formatPence(entry.cashAmountPence)} cash.
                            </>
                          )}
                        </p>
                      )}
                      <p className="text-muted">{new Date(entry.completedAt).toLocaleDateString()}</p>
                    </div>
                    <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded border border-border bg-surface">
                      <Image
                        src={mediaUrl(entry.otherItem.imageUrl)}
                        alt={entry.otherItem.name}
                        fill
                        sizes="64px"
                        className="object-cover"
                      />
                    </div>
                  </Card>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-10">
      <h1 className="text-2xl font-semibold">Edit profile</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex items-center gap-4">
          <div className="h-20 w-20 shrink-0 overflow-hidden rounded-full bg-surface">
            {avatarSrc && (
              // eslint-disable-next-line @next/next/no-img-element -- blob: preview URLs can't go through next/image
              <img src={avatarSrc} alt={user.name} className="h-full w-full object-cover" />
            )}
          </div>
          <label className="flex flex-1 flex-col gap-1 text-sm">
            Profile picture
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              onChange={(event) => setImage(event.target.files?.[0] ?? null)}
              className={inputClassName}
            />
          </label>
        </div>

        <label className="flex flex-col gap-1 text-sm">
          Username
          <input value={user.username} disabled className={`${inputClassName} opacity-60`} />
        </label>

        <label className="flex flex-col gap-1 text-sm">
          Name
          <input
            required
            maxLength={255}
            value={name}
            onChange={(event) => setName(event.target.value)}
            className={inputClassName}
          />
        </label>

        <label className="flex flex-col gap-1 text-sm">
          Bio
          <textarea
            rows={4}
            maxLength={1000}
            value={bio}
            onChange={(event) => setBio(event.target.value)}
            className={inputClassName}
          />
        </label>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex gap-3">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? "Saving…" : "Save changes"}
          </Button>
          <Button type="button" variant="secondary" onClick={handleCancel} disabled={isSubmitting}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
}
