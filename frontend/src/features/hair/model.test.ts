import { describe, expect, it } from "vitest";
import { hairConfigurationSchema, hairPreviewSchema } from "@/features/hair/model";

describe("نماذج تركيب الشعر", () => {
  it("يتحقق من القيم المالية والحد الأدنى للإعداد", () => {
    const valid = { hairItemId: "3", brand: "علامة", length: "60 سم", color: "بني", unitType: "خصلة", unitSellingPrice: "80", unitCost: "45.50", installationFee: "300", minimumQuantity: "5", active: true, effectiveDate: "2026-07-26" };
    expect(hairConfigurationSchema.safeParse(valid).success).toBe(true);
    expect(hairConfigurationSchema.safeParse({ ...valid, minimumQuantity: "0" }).success).toBe(false);
  });

  it("يفرض سببًا للسعر اليدوي", () => {
    const base = { configurationId: "2", quantity: "10", discountId: "", overrideAmount: "", overrideReason: "" };
    expect(hairPreviewSchema.safeParse(base).success).toBe(true);
    expect(hairPreviewSchema.safeParse({ ...base, overrideAmount: "1500" }).success).toBe(false);
    expect(hairPreviewSchema.safeParse({ ...base, overrideAmount: "1500", overrideReason: "موافقة المدير" }).success).toBe(true);
  });
});
