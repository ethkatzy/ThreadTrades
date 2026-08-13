import { apiFetch } from "./client";

export type AuthResponse = {
  token: string;
  userId: number;
  username: string;
  name: string;
};

export type UserProfile = {
  id: number;
  username: string;
  name: string;
  bio: string | null;
  profilePictureUrl: string | null;
};

export type RegisterInput = {
  email: string;
  password: string;
  username: string;
  name: string;
};

export type LoginInput = {
  email: string;
  password: string;
};

export function register(input: RegisterInput): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function login(input: LoginInput): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function getCurrentUser(token: string): Promise<UserProfile> {
  return apiFetch<UserProfile>("/api/users/me", {}, token);
}

export function updateProfile(token: string, formData: FormData): Promise<UserProfile> {
  return apiFetch<UserProfile>("/api/users/me", { method: "PATCH", body: formData }, token);
}
