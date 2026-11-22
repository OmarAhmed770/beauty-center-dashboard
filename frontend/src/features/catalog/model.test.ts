import { describe, expect, it } from "vitest";
import { catalogItemSchema, priceUpdateSchema } from "@/features/catalog/model";

describe("نماذج الكتالوج والأسعار", () => {
  const validItem = { code: "SERVICE_01", nameArabic: "قص شعر", nameEnglish: "Hair cut", itemType: "SERVICE", categoryId: "1", currentSellingPrice: "250.00", currentCost: "0", active: true, description: "" } as const;

  it("يتحقق من الرمز والأسعار عند إنشاء الصنف", () => {
    expect(catalogItemSchema.safeParse(validItem).success).toBe(true);
    expect(catalogItemSchema.safeParse({ ...validItem, code: "رمز غير صالح", currentSellingPrice: "-1" }).success).toBe(false);
  });

  it("يتطلب قيمة جديدة وسببًا عند تحديث السعر", () => {
    expect(priceUpdateSchema.safeParse({ sellingPrice: "", cost: "", reason: "" }).success).toBe(false);
    expect(priceUpdateSchema.safeParse({ sellingPrice: "275", cost: "", reason: "تحديث قائمة الأسعار" }).success).toBe(true);
  });
});
