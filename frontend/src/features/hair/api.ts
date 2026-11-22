import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { HairCalculationPreview, HairConfiguration, HairConfigurationInput, HairPriceOverrideAudit, HairStock } from "@/features/hair/types";

export const hairKeys = {
  all: ["hair-configurations"] as const,
  list: (params?: object) => ["hair-configurations", "list", params ?? {}] as const,
  overrides: (params?: object) => ["hair-configurations", "overrides", params ?? {}] as const,
  stock: (hairItemId: number) => ["hair-stock", hairItemId] as const,
};

export async function searchHairConfigurations(params: { hairItemId?: number; active?: boolean; effectiveOn?: string; query?: string; page?: number; size?: number } = {}): Promise<PageResponse<HairConfiguration>> {
  const { data } = await apiClient.get<PageResponse<HairConfiguration>>("/api/v1/hair-configurations", { params });
  return data;
}

export async function getHairStock(hairItemId: number): Promise<HairStock> {
  const { data } = await apiClient.get<HairStock>(`/api/v1/hair-stock/${hairItemId}`);
  return data;
}

export async function initializeHairStock(hairItemId: number, input: { quantityGrams: string; reason: string }): Promise<HairStock> {
  const { data } = await apiClient.post<HairStock>(`/api/v1/hair-stock/${hairItemId}/initialize`, input);
  return data;
}

export async function adjustHairStock(hairItemId: number, input: { adjustmentGrams: string; reason: string }): Promise<HairStock> {
  const { data } = await apiClient.post<HairStock>(`/api/v1/hair-stock/${hairItemId}/adjustments`, input);
  return data;
}

export async function createHairConfiguration(input: HairConfigurationInput): Promise<HairConfiguration> {
  const { data } = await apiClient.post<HairConfiguration>("/api/v1/hair-configurations", { ...input, active: input.active ?? true });
  return data;
}

export async function setHairConfigurationActive(id: number, active: boolean): Promise<HairConfiguration> {
  const { data } = await apiClient.patch<HairConfiguration>(`/api/v1/hair-configurations/${id}/active`, { active });
  return data;
}

export async function previewHairCalculation(input: { configurationId: number; quantity: string; discountId?: number; overrideAmount?: string; overrideReason?: string }): Promise<HairCalculationPreview> {
  const { data } = await apiClient.post<HairCalculationPreview>("/api/v1/hair-configurations/preview", input);
  return data;
}

export async function searchHairOverrides(params: { configurationId?: number; page?: number; size?: number } = {}): Promise<PageResponse<HairPriceOverrideAudit>> {
  const { data } = await apiClient.get<PageResponse<HairPriceOverrideAudit>>("/api/v1/hair-configurations/price-overrides", { params });
  return data;
}
