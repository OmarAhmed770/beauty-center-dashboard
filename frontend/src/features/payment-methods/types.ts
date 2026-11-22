export type PaymentMethodType = "CASH" | "CARD" | "WALLET" | "OTHER";

export interface PaymentMethod {
  id: number;
  code: string;
  displayNameArabic: string;
  displayNameEnglish: string;
  type: PaymentMethodType;
  active: boolean;
  requiresReferenceNumber: boolean;
  sortOrder: number;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface PaymentMethodInput {
  code?: string;
  displayNameArabic: string;
  displayNameEnglish: string;
  type: PaymentMethodType;
  active?: boolean;
  requiresReferenceNumber: boolean;
  sortOrder: number;
}
