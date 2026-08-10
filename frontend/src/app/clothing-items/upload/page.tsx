"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { ApiError } from "@/lib/api/client";
import { uploadClothingItem } from "@/lib/api/clothingItems";

const CLOTHING_SIZES = ["XS", "S", "M", "L", "XL", "XXL"] as const;

const CONDITIONS = [
  { value: "NEW", label: "New" },
  { value: "LIKE_NEW", label: "Like new" },
  { value: "GOOD", label: "Good" },
  { value: "FAIR", label: "Fair" },
  { value: "WORN", label: "Worn" },
] as const;

const GENDERS = [
  { value: "MENS", label: "Men's" },
  { value: "WOMENS", label: "Women's" },
  { value: "UNISEX", label: "Unisex" },
  { value: "KIDS", label: "Kids" },
] as const;

const inputClassName = "rounded border border-black/10 px-3 py-2 dark:border-white/20";
// <select> renders its dropdown popup as a separate browser-native layer that
// doesn't inherit the page's dark background, so without an explicit
// background/text color here the popup falls back to light-on-white and only
// the highlighted option stays readable.
const selectClassName = `${inputClassName} bg-background text-foreground`;

export default function UploadClothingItemPage() {
  const { token, user, isLoading } = useAuth();
  const router = useRouter();

  const [name, setName] = useState("");
  const [brand, setBrand] = useState("");
  const [itemType, setItemType] = useState("");
  const [description, setDescription] = useState("");
  const [clothingSize, setClothingSize] = useState<string>("M");
  const [colour, setColour] = useState("");
  const [condition, setCondition] = useState<string>("GOOD");
  const [gender, setGender] = useState<string>("UNISEX");
  const [image, setImage] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [isLoading, user, router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }
    if (!image) {
      setError("Please choose a photo.");
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      const formData = new FormData();
      formData.append("image", image);
      formData.append("name", name);
      if (brand) {
        formData.append("brand", brand);
      }
      formData.append("itemType", itemType);
      if (description) {
        formData.append("description", description);
      }
      formData.append("clothingSize", clothingSize);
      if (colour) {
        formData.append("colour", colour);
      }
      formData.append("condition", condition);
      formData.append("gender", gender);

      await uploadClothingItem(token, formData);
      router.push("/");
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

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-10">
      <h1 className="text-2xl font-semibold">Upload an item</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <label className="flex flex-col gap-1 text-sm">
          Photo
          <input
            type="file"
            required
            accept="image/jpeg,image/png,image/webp,image/gif"
            onChange={(event) => setImage(event.target.files?.[0] ?? null)}
            className={inputClassName}
          />
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
          Brand
          <input
            maxLength={255}
            value={brand}
            onChange={(event) => setBrand(event.target.value)}
            className={inputClassName}
          />
        </label>

        <label className="flex flex-col gap-1 text-sm">
          Item type
          <input
            required
            maxLength={50}
            placeholder="e.g. Jacket, Jeans, Dress"
            value={itemType}
            onChange={(event) => setItemType(event.target.value)}
            className={inputClassName}
          />
        </label>

        <label className="flex flex-col gap-1 text-sm">
          Description
          <textarea
            rows={3}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            className={inputClassName}
          />
        </label>

        <div className="grid grid-cols-2 gap-4">
          <label className="flex flex-col gap-1 text-sm">
            Size
            <select
              value={clothingSize}
              onChange={(event) => setClothingSize(event.target.value)}
              className={selectClassName}
            >
              {CLOTHING_SIZES.map((size) => (
                <option key={size} value={size}>
                  {size}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 text-sm">
            Colour
            <input
              maxLength={50}
              value={colour}
              onChange={(event) => setColour(event.target.value)}
              className={inputClassName}
            />
          </label>

          <label className="flex flex-col gap-1 text-sm">
            Condition
            <select
              value={condition}
              onChange={(event) => setCondition(event.target.value)}
              className={selectClassName}
            >
              {CONDITIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 text-sm">
            Gender
            <select value={gender} onChange={(event) => setGender(event.target.value)} className={selectClassName}>
              {GENDERS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded bg-foreground px-4 py-2 text-background disabled:opacity-50"
        >
          {isSubmitting ? "Uploading…" : "Upload item"}
        </button>
      </form>
    </div>
  );
}
