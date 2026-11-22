import { describe, expect, it } from "vitest";
import { discountSchema } from "@/features/discounts/model";

const base = { name: "عرض الصيف", type: "PERCENTAGE", value: "10", startDate: "2026-07-01", endDate: "2026-07-31", active: true, scope: "GENERAL", catalogItemId: "", categoryId: "" } as const;

describe("نموذج الخصومات", () => {
  it("يقبل نسبة صحيحة ويرفض ما يزيد على 100", () => {
    expect(discountSchema.safeParse(base).success).toBe(true);
    expect(discountSchema.safeParse({ ...base, value: "100.01" }).success).toBe(false);
  });

  it("يتحقق من ترتيب التواريخ ونطاق الصنف", () => {
    expect(discountSchema.safeParse({ ...base, startDate: "2026-08-01" }).success).toBe(false);
    expect(discountSchema.safeParse({ ...base, scope: "ITEM" }).success).toBe(false);
    expect(discountSchema.safeParse({ ...base, scope: "ITEM", catalogItemId: "4" }).success).toBe(true);
  });
});
