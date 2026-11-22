import { z } from "zod";
import type { InvoicePaymentInput } from "@/features/sales/types";

const moneyPattern = /^\d{1,17}(\.\d{1,2})?$/;
const quantityPattern = /^\d{1,16}$/;

export const draftInvoiceSchema = z.object({
  businessDate: z.string().min(1, "تاريخ العمل مطلوب"),
  customerName: z.string().trim().max(150, "يجب ألا يتجاوز اسم العميل 150 حرفًا"),
  customerPhone: z.string().trim().max(50, "يجب ألا يتجاوز الهاتف 50 حرفًا"),
  notes: z.string().trim().max(2000, "يجب ألا تتجاوز الملاحظات 2000 حرف"),
});

export type DraftInvoiceFormValues = z.infer<typeof draftInvoiceSchema>;

export const simpleOrderSchema = z.object({
  customerName: z.string().trim().max(150, "يجب ألا يتجاوز اسم العميل 150 حرفًا"),
  customerPhone: z.string().trim().max(50, "يجب ألا يتجاوز رقم الهاتف 50 حرفًا"),
  businessDate: z.string().min(1, "تاريخ الأوردر مطلوب"),
  description: z.string().trim().min(1, "اكتب الخدمة أو ما حصل عليه العميل").max(150, "يجب ألا يتجاوز الوصف 150 حرفًا"),
  totalAmount: z.string().trim().regex(moneyPattern, "أدخل مبلغًا صحيحًا").refine((value) => Number(value) > 0, "يجب أن يكون المبلغ أكبر من صفر"),
  paymentMethodId: z.string().min(1, "اختر طريقة الدفع"),
  paymentReference: z.string().trim().max(200, "يجب ألا يتجاوز مرجع الدفع 200 حرف"),
  employeeId: z.string(),
  notes: z.string().trim().max(2000, "يجب ألا تتجاوز الملاحظات 2000 حرف"),
});

export type SimpleOrderFormValues = z.infer<typeof simpleOrderSchema>;

export const invoiceItemSchema = z.object({
  catalogItemId: z.string().min(1, "اختر خدمة أو منتجًا"),
  hairConfigurationId: z.string(),
  quantity: z.string().trim().regex(quantityPattern, "أدخل كمية صحيحة بدون كسور").refine((value) => Number(value) > 0, "يجب أن تكون الكمية أكبر من صفر"),
  discountId: z.string(),
  employeeId: z.string(),
  priceOverrideAmount: z.string().trim().refine((value) => value === "" || moneyPattern.test(value), "أدخل سعرًا يدويًا صحيحًا"),
  priceOverrideReason: z.string().trim().max(500, "يجب ألا يتجاوز السبب 500 حرف"),
}).superRefine((values, context) => {
  if (values.priceOverrideAmount && !values.priceOverrideReason) context.addIssue({ code: "custom", path: ["priceOverrideReason"], message: "سبب تعديل السعر مطلوب" });
  if (!values.priceOverrideAmount && values.priceOverrideReason) context.addIssue({ code: "custom", path: ["priceOverrideAmount"], message: "السعر اليدوي مطلوب عند إدخال السبب" });
});

export type InvoiceItemFormValues = z.infer<typeof invoiceItemSchema>;

export const invoicePaymentEntrySchema = z.object({
  paymentMethodId: z.string().min(1, "طريقة الدفع مطلوبة"),
  amount: z.string().trim().regex(moneyPattern, "أدخل مبلغًا صحيحًا").refine((value) => Number(value) > 0, "يجب أن يكون المبلغ أكبر من صفر"),
  referenceNumber: z.string().trim().max(200, "يجب ألا يتجاوز المرجع 200 حرف"),
});

export type InvoicePaymentEntryValues = z.infer<typeof invoicePaymentEntrySchema>;

export function paymentTotals(finalTotal: number | string, payments: Array<Pick<InvoicePaymentInput, "amount">>) {
  const total = Number(finalTotal) || 0;
  const paid = Math.round(payments.reduce((sum, payment) => sum + (Number(payment.amount) || 0), 0) * 100) / 100;
  const remaining = Math.round((total - paid) * 100) / 100;
  return { total, paid, remaining, matches: remaining === 0 };
}

export const invoiceStatusLabels = {
  DRAFT: "مسودة",
  COMPLETED: "مكتملة",
  CANCELLED: "ملغاة",
  PARTIALLY_REFUNDED: "مستردة جزئيًا",
  REFUNDED: "مستردة بالكامل",
} as const;
