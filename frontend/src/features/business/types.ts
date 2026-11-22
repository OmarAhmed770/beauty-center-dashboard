import type { CurrencyCode } from "@/lib/format";

export interface BusinessProfile {
  businessName: string;
  logoDataUrl: string | null;
  phone: string | null;
  address: string | null;
  currency: CurrencyCode;
  receiptInformation: string | null;
  updatedAt: string | null;
}

export interface UpdateBusinessProfileRequest {
  businessName: string;
  logoDataUrl: string | null;
  phone: string | null;
  address: string | null;
  currency: CurrencyCode;
  receiptInformation: string | null;
}
