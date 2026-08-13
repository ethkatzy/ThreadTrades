"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { ApiError } from "@/lib/api/client";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const inputClassName = "rounded border border-black/10 px-3 py-2 dark:border-white/20";

export default function ProfilePage() {
  const { user, isLoading, updateProfile } = useAuth();
  const router = useRouter();

  const [name, setName] = useState("");
  const [bio, setBio] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

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
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading || !user) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-sm text-zinc-600 dark:text-zinc-400">Loading…</p>
      </div>
    );
  }

  const avatarSrc = previewUrl ?? (user.profilePictureUrl ? `${API_BASE_URL}${user.profilePictureUrl}` : null);

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-10">
      <h1 className="text-2xl font-semibold">Your profile</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex items-center gap-4">
          <div className="h-20 w-20 shrink-0 overflow-hidden rounded-full bg-zinc-100 dark:bg-zinc-900">
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
        {saved && !error && <p className="text-sm text-green-700 dark:text-green-400">Saved.</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded bg-foreground px-4 py-2 text-background disabled:opacity-50"
        >
          {isSubmitting ? "Saving…" : "Save changes"}
        </button>
      </form>
    </div>
  );
}
