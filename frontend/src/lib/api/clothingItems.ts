import { apiFetch } from "./client";

export type ClothingItem = {
  id: number;
  ownerId: number;
  name: string;
  imageUrl: string;
  brand: string | null;
  itemType: string;
  description: string | null;
  clothingSize: string;
  colour: string | null;
  condition: string;
  gender: string;
  status: "AVAILABLE" | "SWAPPED" | "REMOVED";
  createdAt: string;
  ownerAverageRating: number | null;
  ownerReviewCount: number;
};

export function listMyClothingItems(): Promise<ClothingItem[]> {
  return apiFetch<ClothingItem[]>("/api/clothing-items/mine", {});
}

export function getClothingItem(id: number): Promise<ClothingItem> {
  return apiFetch<ClothingItem>(`/api/clothing-items/${id}`, {});
}

export function uploadClothingItem(formData: FormData): Promise<ClothingItem> {
  return apiFetch<ClothingItem>("/api/clothing-items", { method: "POST", body: formData });
}
