import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { Discount, DiscountInput, DiscountType } from "@/features/discounts/types";

export const discountKeys = {
  all: ["discounts"] as const,
  list: (params?: object) => ["discounts", "list", params ?? {}] as const,
};

export async function searchDiscounts(params: { active?: boolean; type?: DiscountType; catalogItemId?: number; categoryId?: number; applicableDate?: string; query?: string; page?: number; size?: number } = {}): Promise<PageResponse<Discount>> {
  const { data } = await apiClient.get<PageResponse<Discount>>("/api/v1/discounts", { params });
  return data;
}

export async function createDiscount(input: DiscountInput): Promise<Discount> {
  const { data } = await apiClient.post<Discount>("/api/v1/discounts", { ...input, active: input.active ?? true });
  return data;
}

export async function updateDiscount(id: number, input: DiscountInput): Promise<Discount> {
  const { active: _active, ...request } = input;
  const { data } = await apiClient.put<Discount>(`/api/v1/discounts/${id}`, request);
  return data;
}

export async function setDiscountActive(id: number, active: boolean): Promise<Discount> {
  const { data } = await apiClient.patch<Discount>(`/api/v1/discounts/${id}/active`, { active });
  return data;
}
