import { apiClient } from "@/lib/api/client";
import type { AccessTokenResponse, CreateInitialOwnerRequest, InitialSetupStatus, LoginRequest, PublicBusinessIdentity, StaffRegistrationRequest, StaffRegistrationResponse, UserResponse } from "@/types/auth";

export async function requestLogin(credentials: LoginRequest): Promise<AccessTokenResponse> {
  const { data } = await apiClient.post<AccessTokenResponse>("/api/v1/auth/login", credentials);
  return data;
}

export async function requestCurrentUser(): Promise<UserResponse> {
  const { data } = await apiClient.get<UserResponse>("/api/v1/auth/me");
  return data;
}

export async function requestInitialSetupStatus(): Promise<InitialSetupStatus> {
  const { data } = await apiClient.get<InitialSetupStatus>("/api/v1/setup/status");
  return data;
}

export async function createInitialOwner(request: CreateInitialOwnerRequest): Promise<UserResponse> {
  const { data } = await apiClient.post<UserResponse>("/api/v1/setup/owner", request);
  return data;
}

export async function requestStaffRegistration(request: StaffRegistrationRequest): Promise<StaffRegistrationResponse> {
  const { data } = await apiClient.post<StaffRegistrationResponse>("/api/v1/auth/register", request);
  return data;
}

export async function requestPublicBusinessIdentity(): Promise<PublicBusinessIdentity> {
  const { data } = await apiClient.get<PublicBusinessIdentity>("/api/v1/business-profile/public");
  return data;
}
