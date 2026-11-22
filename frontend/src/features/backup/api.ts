import type { BackupConfiguration, BackupHistoryItem, BackupStatus } from "@/features/backup/types";
import { apiClient } from "@/lib/api/client";

export const backupKeys = { status: ["backups", "status"] as const, configuration: ["backups", "configuration"] as const, history: ["backups", "history"] as const };

export async function getBackupStatus(): Promise<BackupStatus> {
  const { data } = await apiClient.get<BackupStatus>("/api/v1/backups/status");
  return data;
}

export async function runManualBackup(): Promise<BackupStatus> {
  const { data } = await apiClient.post<BackupStatus>("/api/v1/backups/manual");
  return data;
}

export async function getBackupConfiguration(): Promise<BackupConfiguration> {
  const { data } = await apiClient.get<BackupConfiguration>("/api/v1/backups/configuration");
  return data;
}

export async function updateBackupConfiguration(input: BackupConfiguration): Promise<BackupConfiguration> {
  const { data } = await apiClient.put<BackupConfiguration>("/api/v1/backups/configuration", input);
  return data;
}

export async function getBackupHistory(): Promise<BackupHistoryItem[]> {
  const { data } = await apiClient.get<BackupHistoryItem[]>("/api/v1/backups/history");
  return data;
}

export async function deleteBackup(id: number): Promise<void> { await apiClient.delete(`/api/v1/backups/${id}`); }
export async function openBackupFolder(): Promise<void> { await apiClient.post("/api/v1/backups/open-folder"); }
export async function restoreBackup(id: number, ownerPassword: string, confirmation: string): Promise<BackupStatus> {
  const { data } = await apiClient.post<BackupStatus>(`/api/v1/backups/${id}/restore`, { ownerPassword, confirmation });
  return data;
}
