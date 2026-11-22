import { describe, expect, it } from "vitest";
import { draftInvoiceSchema, invoiceItemSchema, invoicePaymentEntrySchema, paymentTotals } from "@/features/sales/model";

describe("نماذج الفاتورة والتحصيل", () => {
  it("يتحقق من بيانات المسودة والكمية", () => {
    expect(draftInvoiceSchema.safeParse({ businessDate: "2026-07-26", customerName: "", customerPhone: "", notes: "" }).success).toBe(true);
    expect(invoiceItemSchema.safeParse({ catalogItemId: "1", hairConfigurationId: "", quantity: "0", discountId: "", employeeId: "", priceOverrideAmount: "", priceOverrideReason: "" }).success).toBe(false);
  });

  it("يفرض مبلغ دفع موجبًا", () => {
    expect(invoicePaymentEntrySchema.safeParse({ paymentMethodId: "1", amount: "500", referenceNumber: "" }).success).toBe(true);
    expect(invoicePaymentEntrySchema.safeParse({ paymentMethodId: "1", amount: "0", referenceNumber: "" }).success).toBe(false);
  });

  it("يمنع الإكمال حسابيًا عند عدم تطابق مجموع الدفعات", () => {
    expect(paymentTotals("1500.00", [{ amount: "500" }, { amount: "1000" }])).toEqual({ total: 1500, paid: 1500, remaining: 0, matches: true });
    expect(paymentTotals("1500.00", [{ amount: "500" }]).matches).toBe(false);
  });
});
