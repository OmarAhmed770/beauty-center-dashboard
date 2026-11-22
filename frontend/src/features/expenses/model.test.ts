import { describe, expect, it } from "vitest";
import { createExpenseSchema, paymentMethodSchema } from "@/features/expenses/model";

const validExpense = {
  categoryId: "1",
  amount: "125.50",
  businessDate: "2026-07-26",
  paymentMethodId: "2",
  description: "مستلزمات ضيافة",
  notes: "",
};

describe("نماذج المصروفات", () => {
  it("يقبل طريقة دفع تتطلب مرجعًا دون طلبه في النموذج المبسط", () => {
    expect(createExpenseSchema.safeParse(validExpense).success).toBe(true);
  });

  it("يرفض مبلغًا غير موجب ويقبل المبلغ الصحيح", () => {
    expect(createExpenseSchema.safeParse({ ...validExpense, amount: "0" }).success).toBe(false);
    expect(createExpenseSchema.safeParse(validExpense).success).toBe(true);
  });

  it("يتحقق من رمز وترتيب طريقة الدفع", () => {
    const base = { code: "MOBILE_WALLET", displayNameArabic: "محفظة", displayNameEnglish: "Wallet", type: "WALLET", active: true, requiresReferenceNumber: true, sortOrder: 10 } as const;
    expect(paymentMethodSchema.safeParse(base).success).toBe(true);
    expect(paymentMethodSchema.safeParse({ ...base, code: "رمز غير صالح", sortOrder: -1 }).success).toBe(false);
  });
});
