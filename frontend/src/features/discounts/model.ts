import { z } from "zod";

const moneyPattern = /^\d{1,17}(\.\d{1,2})?$/;

export const discountSchema = z.object({
  name: z.string().trim().min(1, "اسم الخصم مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  type: z.enum(["PERCENTAGE", "FIXED_AMOUNT"]),
  value: z.string().trim().regex(moneyPattern, "أدخل قيمة صحيحة بمنزلتين عشريتين كحد أقصى"),
  startDate: z.string().min(1, "تاريخ البداية مطلوب"),
  endDate: z.string().min(1, "تاريخ النهاية مطلوب"),
  active: z.boolean(),
  scope: z.enum(["GENERAL", "ITEM", "CATEGORY"]),
  catalogItemId: z.string(),
  categoryId: z.string(),
}).superRefine((values, context) => {
  if (values.type === "PERCENTAGE" && Number(values.value) > 100) context.addIssue({ code: "custom", path: ["value"], message: "يجب أن تكون النسبة بين 0 و100" });
  if (values.startDate && values.endDate && values.startDate > values.endDate) context.addIssue({ code: "custom", path: ["endDate"], message: "يجب ألا يسبق تاريخ النهاية تاريخ البداية" });
  if (values.scope === "ITEM" && !values.catalogItemId) context.addIssue({ code: "custom", path: ["catalogItemId"], message: "اختر صنفًا لتطبيق الخصم" });
  if (values.scope === "CATEGORY" && !values.categoryId) context.addIssue({ code: "custom", path: ["categoryId"], message: "اختر تصنيفًا لتطبيق الخصم" });
});

export type DiscountFormValues = z.infer<typeof discountSchema>;

export const discountTypeLabels = { PERCENTAGE: "نسبة مئوية", FIXED_AMOUNT: "مبلغ ثابت" } as const;
