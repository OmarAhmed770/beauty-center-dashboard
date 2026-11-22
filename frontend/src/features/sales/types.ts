import type { CatalogItemType } from "@/features/catalog/types";
import type { DiscountType } from "@/features/discounts/types";
import type { PaymentMethodType } from "@/features/payment-methods/types";
import type { MoneyValue } from "@/lib/format";

export type InvoiceStatus = "DRAFT" | "COMPLETED" | "CANCELLED" | "PARTIALLY_REFUNDED" | "REFUNDED";

export interface InvoiceItem {
  id: number;
  catalogItemId: number | null;
  manualItem: boolean;
  hairConfigurationId: number | null;
  itemCode: string;
  itemNameArabic: string;
  itemNameEnglish: string;
  itemType: CatalogItemType;
  quantity: MoneyValue;
  unitOriginalSellingPrice: MoneyValue;
  unitCostSnapshot: MoneyValue | null;
  installationFeeSnapshot: MoneyValue;
  discountId: number | null;
  discountName: string | null;
  discountType: DiscountType | null;
  discountValue: MoneyValue | null;
  discountAmount: MoneyValue;
  lineSubtotal: MoneyValue;
  finalLineTotal: MoneyValue;
  employeeId: number | null;
  employeeName: string | null;
  priceOverridden: boolean;
  priceOverrideOriginalAmount: MoneyValue | null;
  priceOverrideAmount: MoneyValue | null;
  priceOverrideReason: string | null;
  priceOverriddenBy: string | null;
  priceOverriddenAt: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface InvoicePayment {
  id: number;
  paymentMethodId: number;
  paymentMethodCode: string;
  paymentMethodNameArabic: string;
  paymentMethodNameEnglish: string;
  paymentMethodType: PaymentMethodType;
  amount: MoneyValue;
  referenceNumber: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface InvoiceRefundPayment {
  id: number;
  paymentMethodId: number;
  paymentMethodCode: string;
  paymentMethodNameArabic: string;
  paymentMethodNameEnglish: string;
  paymentMethodType: PaymentMethodType;
  amount: MoneyValue;
  referenceNumber: string | null;
}

export interface InvoiceRefund {
  id: number;
  invoiceId: number;
  businessDate: string;
  amount: MoneyValue;
  reason: string;
  status: "COMPLETED";
  payments: InvoiceRefundPayment[];
  createdAt: string;
  createdBy: string | null;
}

export interface InvoiceDetails {
  id: number;
  invoiceNumber: string;
  businessDate: string;
  customerName: string | null;
  customerPhone: string | null;
  status: InvoiceStatus;
  subtotal: MoneyValue;
  discountTotal: MoneyValue;
  finalTotal: MoneyValue;
  refundedTotal: MoneyValue;
  notes: string | null;
  items: InvoiceItem[];
  payments: InvoicePayment[];
  refunds: InvoiceRefund[];
  createdBy: string | null;
  createdAt: string;
  updatedBy: string | null;
  updatedAt: string;
  completedBy: string | null;
  completedAt: string | null;
  cancelledBy: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
}

export interface InvoiceSummary {
  id: number;
  invoiceNumber: string;
  businessDate: string;
  customerName: string | null;
  customerPhone: string | null;
  status: InvoiceStatus;
  subtotal: MoneyValue;
  discountTotal: MoneyValue;
  finalTotal: MoneyValue;
  refundedTotal: MoneyValue;
  createdBy: string | null;
  createdAt: string;
  completedBy: string | null;
  completedAt: string | null;
}

export interface InvoiceTotalsPreview {
  invoiceId: number;
  subtotal: MoneyValue;
  discountTotal: MoneyValue;
  finalTotal: MoneyValue;
  paymentTotal: MoneyValue;
  remainingPayment: MoneyValue;
}

export interface InvoiceItemInput {
  catalogItemId: number;
  hairConfigurationId?: number;
  quantity: string;
  discountId?: number;
  employeeId?: number;
  priceOverrideAmount?: string;
  priceOverrideReason?: string;
}

export interface InvoicePaymentInput {
  paymentMethodId: number;
  amount: string;
  referenceNumber?: string;
}

export interface SimpleOrderInput {
  customerName?: string;
  customerPhone?: string;
  businessDate: string;
  description: string;
  totalAmount: string;
  paymentMethodId: number;
  paymentReference?: string;
  employeeId?: number;
  notes?: string;
}

export interface QuickOrderLineInput {
  catalogItemId: number;
  quantity: string;
  employeeId?: number;
  hairConfigurationId?: number;
  hairGrams?: string;
}

export interface QuickOrderPaymentInput {
  paymentMethodId: number;
  referenceNumber?: string;
}

export interface QuickOrderInput {
  businessDate: string;
  lines: QuickOrderLineInput[];
  payments: QuickOrderPaymentInput[];
  clientRequestId: string;
  customerName?: string;
  customerPhone?: string;
  notes?: string;
}

export interface QuickOrderResponse {
  invoiceId: number;
  invoiceNumber: string;
  total: MoneyValue;
  lines: Array<{
    catalogItemId: number | null;
    itemNameArabic: string;
    quantity: MoneyValue;
    finalLineTotal: MoneyValue;
    employeeId: number | null;
    employeeName: string | null;
  }>;
}
