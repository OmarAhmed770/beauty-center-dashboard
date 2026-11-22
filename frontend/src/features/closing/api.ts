import type { DailyClosing, DailyClosingPreview } from "@/features/closing/types";
import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";

export const closingKeys = {
  all: ["daily-closings"] as const,
  preview: (date: string) => ["daily-closings", "preview", date] as const,
  stored: (date: string) => ["daily-closings", "stored", date] as const,
  versions: (date: string) => ["daily-closings", "versions", date] as const,
  latest: ["daily-closings", "latest"] as const,
};

export async function getLatestDailyClosing(): Promise<DailyClosing | null> {
  // The existing endpoint sorts by business date, then closing version, descending.
  const { data } = await apiClient.get<PageResponse<DailyClosing>>("/api/v1/daily-closings", { params: { page: 0, size: 1 } });
  return data.content[0] ?? null;
}

export async function getDailyClosingPreview(date: string): Promise<DailyClosingPreview> {
  const { data } = await apiClient.get<DailyClosingPreview>(`/api/v1/daily-closings/${date}/preview`);
  return data;
}

export async function getStoredDailyClosing(date: string): Promise<DailyClosing> {
  const { data } = await apiClient.get<DailyClosing>(`/api/v1/daily-closings/${date}`);
  return data;
}

export async function getDailyClosingVersions(date: string): Promise<DailyClosing[]> {
  const { data } = await apiClient.get<DailyClosing[]>(`/api/v1/daily-closings/${date}/versions`);
  return data;
}

export async function closeDaily(date: string, input: { actualCash: string; notes?: string }): Promise<DailyClosing> {
  const { data } = await apiClient.post<DailyClosing>(`/api/v1/daily-closings/${date}/close`, input);
  return data;
}

export async function reopenDaily(date: string, reason: string): Promise<DailyClosing> {
  const { data } = await apiClient.post<DailyClosing>(`/api/v1/daily-closings/${date}/reopen`, { reason });
  return data;
}
