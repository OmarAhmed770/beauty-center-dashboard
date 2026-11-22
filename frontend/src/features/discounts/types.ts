import type { MoneyValue } from "@/lib/format";

export type DiscountType = "PERCENTAGE" | "FIXED_AMOUNT";

export interface Discount {
  id: number;
  name: string;
  type: DiscountType;
  value: MoneyValue;
  startDate: string;
  endDate: string;
  active: boolean;
  catalogItemId: number | null;
  catalogItemCode: string | null;
  categoryId: number | null;
  categoryNameArabic: string | null;
  categoryNameEnglish: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface DiscountInput {
  name: string;
  type: DiscountType;
  value: string;
  startDate: string;
  endDate: string;
  active?: boolean;
  catalogItemId?: number;
  categoryId?: number;
}
