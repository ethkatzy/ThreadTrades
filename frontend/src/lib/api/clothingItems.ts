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
  status: "AVAILABLE" | "SWAPPED";
  createdAt: string;
  ownerAverageRating: number | null;
  ownerReviewCount: number;
};

export function listMyClothingItems(token: string): Promise<ClothingItem[]> {
  return apiFetch<ClothingItem[]>("/api/clothing-items/mine", {}, token);
}

export function getClothingItem(token: string, id: number): Promise<ClothingItem> {
  return apiFetch<ClothingItem>(`/api/clothing-items/${id}`, {}, token);
}

export function uploadClothingItem(token: string, formData: FormData): Promise<ClothingItem> {
  return apiFetch<ClothingItem>("/api/clothing-items", { method: "POST", body: formData }, token);
}
