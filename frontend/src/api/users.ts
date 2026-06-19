import { apiRequest } from "./client";
import type { AuthUser } from "./auth";

export interface UserProfile extends AuthUser {
  interests: string[];
}

export function getMyProfile(): Promise<UserProfile> {
  return apiRequest("/users/me");
}

export interface UpdateProfileInput {
  username?: string;
  email?: string;
  interests?: string[];
}

export function updateProfile(data: UpdateProfileInput): Promise<UserProfile> {
  return apiRequest("/users/me", {
    method: "PATCH",
    body: JSON.stringify(data),
  });
}
