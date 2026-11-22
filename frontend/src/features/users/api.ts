import { apiClient } from "@/lib/api/client";
import type { SystemPermission, UserResponse, UserRole } from "@/types/auth";

export type EmployeeLinkMode = "AUTO_CREATE" | "EXISTING" | "NONE";

export interface UserDraft {
  username?: string;
  displayName: string;
  password?: string;
  roles: UserRole[];
  permissions?: SystemPermission[];
  employeeLinkMode?: EmployeeLinkMode;
  employeeId?: number;
}

export interface RegistrationApprovalInput {
  role: Extract<UserRole, "RECEPTIONIST">;
  employeeLinkMode?: EmployeeLinkMode;
  employeeId?: number;
}

export const userKeys = {
  all: ["users"] as const,
  pending: ["users", "pending-registrations"] as const,
};

export async function getUsers(): Promise<UserResponse[]> {
  const { data } = await apiClient.get<UserResponse[]>("/api/v1/users");
  return data;
}

export async function createUser(draft: UserDraft & { username: string; password: string }): Promise<UserResponse> {
  const { data } = await apiClient.post<UserResponse>("/api/v1/users", draft);
  return data;
}

export async function updateUser(userId: number, draft: Pick<UserDraft, "displayName" | "roles" | "permissions">): Promise<UserResponse> {
  const { data } = await apiClient.put<UserResponse>(`/api/v1/users/${userId}`, draft);
  return data;
}

export async function setUserActive(userId: number, active: boolean): Promise<UserResponse> {
  const { data } = await apiClient.patch<UserResponse>(`/api/v1/users/${userId}/active`, { active });
  return data;
}

export async function resetUserPassword(userId: number, password: string): Promise<UserResponse> {
  const { data } = await apiClient.post<UserResponse>(`/api/v1/users/${userId}/password-reset`, { password });
  return data;
}

export async function getPendingRegistrations(): Promise<UserResponse[]> {
  const { data } = await apiClient.get<UserResponse[]>("/api/v1/users/pending-registrations");
  return data;
}

export async function approveRegistration(userId: number, input: RegistrationApprovalInput): Promise<UserResponse> {
  const { data } = await apiClient.patch<UserResponse>(`/api/v1/users/${userId}/registration/approve`, input);
  return data;
}

export async function rejectRegistration(userId: number): Promise<UserResponse> {
  const { data } = await apiClient.patch<UserResponse>(`/api/v1/users/${userId}/registration/reject`);
  return data;
}
