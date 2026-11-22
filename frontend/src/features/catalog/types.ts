import type { MoneyValue } from "@/lib/format";

export type CatalogItemType = "SERVICE" | "PRODUCT" | "HAIR_PRODUCT" | "HAIR_INSTALLATION";
export type PriceValueType = "SELLING_PRICE" | "COST";

export interface CatalogCategory {
  id: number;
  nameArabic: string;
  nameEnglish: string;
  active: boolean;
  description: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface CatalogItem {
  id: number;
  code: string;
  nameArabic: string;
  nameEnglish: string;
  itemType: CatalogItemType;
  categoryId: number;
  categoryNameArabic: string;
  categoryNameEnglish: string;
  currentSellingPrice: MoneyValue;
  currentCost: MoneyValue | null;
  active: boolean;
  description: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface CatalogCategoryInput {
  nameArabic: string;
  nameEnglish: string;
  active?: boolean;
  description?: string;
}

export interface CatalogItemInput {
  code: string;
  nameArabic: string;
  nameEnglish: string;
  itemType: CatalogItemType;
  categoryId: number;
  currentSellingPrice: string;
  currentCost: string;
  active?: boolean;
  description?: string;
}

export interface CatalogItemUpdateInput {
  nameArabic: string;
  nameEnglish: string;
  categoryId: number;
  description?: string;
}

export interface PriceHistory {
  id: number;
  catalogItemId: number;
  catalogItemCode: string;
  catalogItemNameArabic: string;
  catalogItemNameEnglish: string;
  valueType: PriceValueType;
  oldValue: MoneyValue;
  newValue: MoneyValue;
  effectiveAt: string;
  changeReason: string;
  changedBy: string;
  createdAt: string;
}
