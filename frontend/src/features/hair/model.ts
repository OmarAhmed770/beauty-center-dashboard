import { z } from "zod";

const moneyPattern = /^\d{1,17}(\.\d{1,2})?$/;
const wholeQuantityPattern = /^\d{1,16}$/;

export const hairConfigurationSchema = z.object({
  hairItemId: z.string().min(1, "منتج الشعر مطلوب"),
  brand: z.string().trim().min(1, "العلامة التجارية مطلوبة").max(150, "يجب ألا تتجاوز العلامة 150 حرفًا"),
  length: z.string().trim().min(1, "الطول مطلوب").max(100, "يجب ألا يتجاوز الطول 100 حرف"),
  color: z.string().trim().max(100, "يجب ألا يتجاوز اللون 100 حرف"),
  unitType: z.string().trim().min(1, "نوع الوحدة مطلوب").max(50, "يجب ألا يتجاوز نوع الوحدة 50 حرفًا"),
  unitSellingPrice: z.string().trim().regex(moneyPattern, "أدخل سعر بيع صحيحًا"),
  unitCost: z.string().trim().regex(moneyPattern, "أدخل سعر شراء صحيحًا"),
  installationFee: z.string().trim().regex(moneyPattern, "أدخل رسوم تركيب صحيحة"),
  minimumQuantity: z.string().trim().refine((value) => value === "" || (wholeQuantityPattern.test(value) && Number(value) > 0), "أدخل حدًا أدنى صحيحًا بالجرام بدون كسور"),
  active: z.boolean(),
  effectiveDate: z.string().min(1, "تاريخ السريان مطلوب"),
});

export type HairConfigurationFormValues = z.infer<typeof hairConfigurationSchema>;

export const hairPreviewSchema = z.object({
  configurationId: z.string().min(1, "إعداد الشعر مطلوب"),
  quantity: z.string().trim().regex(wholeQuantityPattern, "أدخل عدد جرامات صحيح بدون كسور").refine((value) => Number(value) > 0, "يجب أن تكون الكمية أكبر من صفر"),
  discountId: z.string(),
  overrideAmount: z.string().trim().refine((value) => value === "" || moneyPattern.test(value), "أدخل سعرًا يدويًا صحيحًا"),
  overrideReason: z.string().trim().max(500, "يجب ألا يتجاوز السبب 500 حرف"),
}).superRefine((values, context) => {
  if (values.overrideAmount && !values.overrideReason) context.addIssue({ code: "custom", path: ["overrideReason"], message: "سبب تعديل السعر مطلوب" });
  if (!values.overrideAmount && values.overrideReason) context.addIssue({ code: "custom", path: ["overrideAmount"], message: "السعر اليدوي مطلوب عند إدخال السبب" });
});

export type HairPreviewFormValues = z.infer<typeof hairPreviewSchema>;
