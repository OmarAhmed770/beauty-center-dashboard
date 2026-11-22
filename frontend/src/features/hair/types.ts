import type { DiscountType } from "@/features/discounts/types";
import type { MoneyValue } from "@/lib/format";

export interface HairConfiguration {
  id: number;
  hairItemId: number;
  hairItemCode: string;
  hairItemNameArabic: string;
  hairItemNameEnglish: string;
  brand: string;
  length: string;
  color: string | null;
  unitType: string;
  unitSellingPrice: MoneyValue;
  unitCost: MoneyValue | null;
  installationFee: MoneyValue;
  minimumQuantity: MoneyValue | null;
  stockConfigured: boolean;
  availableStockGrams: MoneyValue | null;
  active: boolean;
  effectiveDate: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface HairStock {
  hairItemId: number;
  hairItemCode: string;
  hairItemNameArabic: string;
  stockConfigured: boolean;
  availableGrams: MoneyValue | null;
  version: number;
}

export interface HairStockMovement {
  id: number;
  movementType: "INITIAL" | "MANUAL_ADJUSTMENT" | "SALE" | "CANCELLATION_RESTORE" | "REFUND_RESTORE";
  previousGrams: MoneyValue;
  adjustmentGrams: MoneyValue;
  newGrams: MoneyValue;
  reason: string;
  createdBy: string;
  createdAt: string;
}

export interface HairConfigurationInput {
  hairItemId: number;
  brand: string;
  length: string;
  color?: string;
  unitType: string;
  unitSellingPrice: string;
  unitCost: string;
  installationFee: string;
  minimumQuantity?: string;
  active?: boolean;
  effectiveDate: string;
}

export interface HairCalculationPreview {
  configurationId: number;
  hairItemId: number;
  hairItemCode: string;
  quantity: MoneyValue;
  calculationDate: string;
  baseProductAmount: MoneyValue;
  installationFee: MoneyValue;
  subtotal: MoneyValue;
  discountId: number | null;
  discountType: DiscountType | null;
  discountValue: MoneyValue | null;
  discountAmount: MoneyValue;
  calculatedFinalAmount: MoneyValue;
  overridden: boolean;
  finalAmount: MoneyValue;
  costEstimate: MoneyValue | null;
}

export interface HairPriceOverrideAudit {
  id: number;
  configurationId: number;
  hairItemId: number;
  hairItemCode: string;
  quantity: MoneyValue;
  originalAmount: MoneyValue;
  overriddenAmount: MoneyValue;
  overrideReason: string;
  authorizedBy: string;
  occurredAt: string;
}
