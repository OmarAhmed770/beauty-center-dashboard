import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { CatalogCategory, CatalogCategoryInput, CatalogItem, CatalogItemInput, CatalogItemType, CatalogItemUpdateInput, PriceHistory, PriceValueType } from "@/features/catalog/types";

export const catalogKeys = {
  all: ["catalog"] as const,
  categories: (params?: object) => ["catalog", "categories", params ?? {}] as const,
  items: (params?: object) => ["catalog", "items", params ?? {}] as const,
  item: (id: number) => ["catalog", "items", "detail", id] as const,
  priceHistory: (params?: object) => ["catalog", "price-history", params ?? {}] as const,
};

export async function searchCatalogCategories(params: { query?: string; active?: boolean; page?: number; size?: number } = {}): Promise<PageResponse<CatalogCategory>> {
  const { data } = await apiClient.get<PageResponse<CatalogCategory>>("/api/v1/catalog/categories", { params });
  return data;
}

export async function createCatalogCategory(input: CatalogCategoryInput): Promise<CatalogCategory> {
  const { data } = await apiClient.post<CatalogCategory>("/api/v1/catalog/categories", { ...input, active: input.active ?? true });
  return data;
}

export async function updateCatalogCategory(id: number, input: CatalogCategoryInput): Promise<CatalogCategory> {
  const { active: _active, ...request } = input;
  const { data } = await apiClient.put<CatalogCategory>(`/api/v1/catalog/categories/${id}`, request);
  return data;
}

export async function setCatalogCategoryActive(id: number, active: boolean): Promise<CatalogCategory> {
  const { data } = await apiClient.patch<CatalogCategory>(`/api/v1/catalog/categories/${id}/active`, { active });
  return data;
}

export async function searchCatalogItems(params: { categoryId?: number; itemType?: CatalogItemType; active?: boolean; query?: string; page?: number; size?: number } = {}): Promise<PageResponse<CatalogItem>> {
  const { data } = await apiClient.get<PageResponse<CatalogItem>>("/api/v1/catalog/items", { params });
  return data;
}

export async function getCatalogItem(id: number): Promise<CatalogItem> {
  const { data } = await apiClient.get<CatalogItem>(`/api/v1/catalog/items/${id}`);
  return data;
}

export async function createCatalogItem(input: CatalogItemInput): Promise<CatalogItem> {
  const { data } = await apiClient.post<CatalogItem>("/api/v1/catalog/items", { ...input, active: input.active ?? true });
  return data;
}

export async function updateCatalogItem(id: number, input: CatalogItemUpdateInput): Promise<CatalogItem> {
  const { data } = await apiClient.put<CatalogItem>(`/api/v1/catalog/items/${id}`, input);
  return data;
}

export async function updateCatalogPrices(id: number, input: { sellingPrice?: string; cost?: string; reason: string }): Promise<CatalogItem> {
  const { data } = await apiClient.post<CatalogItem>(`/api/v1/catalog/items/${id}/prices`, input);
  return data;
}

export async function setCatalogItemActive(id: number, active: boolean): Promise<CatalogItem> {
  const { data } = await apiClient.patch<CatalogItem>(`/api/v1/catalog/items/${id}/active`, { active });
  return data;
}

export async function searchPriceHistory(params: { catalogItemId?: number; valueType?: PriceValueType; fromDateTime?: string; toDateTime?: string; page?: number; size?: number } = {}): Promise<PageResponse<PriceHistory>> {
  const { data } = await apiClient.get<PageResponse<PriceHistory>>("/api/v1/price-history", { params });
  return data;
}
