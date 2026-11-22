import { z } from "zod";

const moneyPattern = /^\d{1,17}(\.\d{1,2})?$/;

export const catalogCategorySchema = z.object({
  nameArabic: z.string().trim().min(1, "الاسم العربي مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  nameEnglish: z.string().trim().min(1, "الاسم الإنجليزي مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  active: z.boolean(),
  description: z.string().trim().max(1000, "يجب ألا يتجاوز الوصف 1000 حرف"),
});

export type CatalogCategoryFormValues = z.infer<typeof catalogCategorySchema>;

export const catalogItemSchema = z.object({
  code: z.string().trim().min(1, "رمز الصنف مطلوب").max(50, "يجب ألا يتجاوز الرمز 50 حرفًا").regex(/^[A-Za-z0-9_]+$/, "استخدم حروفًا إنجليزية وأرقامًا وشرطة سفلية فقط"),
  nameArabic: z.string().trim().min(1, "الاسم العربي مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  nameEnglish: z.string().trim().min(1, "الاسم الإنجليزي مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  itemType: z.enum(["SERVICE", "PRODUCT", "HAIR_PRODUCT", "HAIR_INSTALLATION"]),
  categoryId: z.string().min(1, "التصنيف مطلوب"),
  currentSellingPrice: z.string().trim().regex(moneyPattern, "أدخل سعرًا صحيحًا بمنزلتين عشريتين كحد أقصى"),
  currentCost: z.string().trim().regex(moneyPattern, "أدخل سعر شراء صحيحًا بمنزلتين عشريتين كحد أقصى"),
  active: z.boolean(),
  description: z.string().trim().max(2000, "يجب ألا يتجاوز الوصف 2000 حرف"),
});

export type CatalogItemFormValues = z.infer<typeof catalogItemSchema>;

const optionalMoney = z.string().trim().refine((value) => value === "" || moneyPattern.test(value), "أدخل مبلغًا صحيحًا بمنزلتين عشريتين كحد أقصى");

export const priceUpdateSchema = z.object({
  sellingPrice: optionalMoney,
  cost: optionalMoney,
  reason: z.string().trim().min(1, "سبب التغيير مطلوب").max(500, "يجب ألا يتجاوز السبب 500 حرف"),
}).refine((values) => values.sellingPrice !== "" || values.cost !== "", { message: "أدخل سعر بيع أو سعر شراء جديد", path: ["sellingPrice"] });

export type PriceUpdateFormValues = z.infer<typeof priceUpdateSchema>;

export const catalogItemTypeLabels = {
  SERVICE: "خدمة",
  PRODUCT: "منتج",
  HAIR_PRODUCT: "منتج شعر",
  HAIR_INSTALLATION: "تركيب شعر",
} as const;

export const priceValueTypeLabels = { SELLING_PRICE: "سعر البيع", COST: "سعر الشراء" } as const;
