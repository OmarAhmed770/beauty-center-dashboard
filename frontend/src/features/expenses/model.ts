import { z } from "zod";

const moneyPattern = /^\d{1,17}(\.\d{1,2})?$/;

export const createExpenseSchema = z.object({
  categoryId: z.string().min(1, "تصنيف المصروف مطلوب"),
  amount: z.string().trim().regex(moneyPattern, "أدخل مبلغًا صحيحًا بمنزلتين عشريتين كحد أقصى").refine((value) => Number(value) > 0, "يجب أن يكون المبلغ أكبر من صفر"),
  businessDate: z.string().min(1, "تاريخ المصروف مطلوب"),
  paymentMethodId: z.string().min(1, "طريقة الدفع مطلوبة"),
  description: z.string().trim().min(1, "وصف المصروف مطلوب").max(1000, "يجب ألا يتجاوز الوصف 1000 حرف"),
  notes: z.string().trim().max(2000, "يجب ألا تتجاوز الملاحظات 2000 حرف"),
});

export type ExpenseFormValues = z.infer<typeof createExpenseSchema>;

export const expenseCategorySchema = z.object({
  nameArabic: z.string().trim().min(1, "الاسم العربي مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  nameEnglish: z.string().trim().min(1, "الاسم الإنجليزي مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  active: z.boolean(),
  notes: z.string().trim().max(2000, "يجب ألا تتجاوز الملاحظات 2000 حرف"),
});

export type ExpenseCategoryFormValues = z.infer<typeof expenseCategorySchema>;

export const paymentMethodSchema = z.object({
  code: z.string().trim().min(1, "رمز طريقة الدفع مطلوب").max(50, "يجب ألا يتجاوز الرمز 50 حرفًا").regex(/^[A-Za-z0-9_]+$/, "استخدم حروفًا إنجليزية وأرقامًا وشرطة سفلية فقط"),
  displayNameArabic: z.string().trim().min(1, "الاسم العربي مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  displayNameEnglish: z.string().trim().min(1, "الاسم الإنجليزي مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  type: z.enum(["CASH", "CARD", "WALLET", "OTHER"]),
  active: z.boolean(),
  requiresReferenceNumber: z.boolean(),
  sortOrder: z.number().int("الترتيب رقم صحيح").min(0, "الترتيب لا يقل عن صفر").max(10000, "الترتيب لا يتجاوز 10000"),
});

export type PaymentMethodFormValues = z.infer<typeof paymentMethodSchema>;

export const paymentMethodTypeLabels = {
  CASH: "نقدي",
  CARD: "بطاقة",
  WALLET: "محفظة إلكترونية",
  OTHER: "أخرى",
} as const;
