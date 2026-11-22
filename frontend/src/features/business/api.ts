import { apiClient } from "@/lib/api/client";
import type { BusinessProfile, UpdateBusinessProfileRequest } from "@/features/business/types";

export const businessProfileKeys = {
  current: ["business-profile"] as const,
};

export async function getBusinessProfile(): Promise<BusinessProfile> {
  const { data } = await apiClient.get<BusinessProfile>("/api/v1/business-profile");
  return data;
}

export async function updateBusinessProfile(request: UpdateBusinessProfileRequest): Promise<BusinessProfile> {
  const { data } = await apiClient.put<BusinessProfile>("/api/v1/business-profile", request);
  return data;
}
