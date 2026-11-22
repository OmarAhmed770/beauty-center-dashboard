import axios from "axios";
import type { ApiErrorResponse, ValidationErrorResponse } from "@/types/api";

export class ApiClientError extends Error {
  readonly status?: number;
  readonly code?: string;
  readonly path?: string;
  readonly validationErrors: ValidationErrorResponse[];

  constructor(
    message: string,
    options: {
      status?: number;
      code?: string;
      path?: string;
      validationErrors?: ValidationErrorResponse[];
    } = {},
  ) {
    super(message);
    this.name = "ApiClientError";
    this.status = options.status;
    this.code = options.code;
    this.path = options.path;
    this.validationErrors = options.validationErrors ?? [];
  }
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (!value || typeof value !== "object") return false;
  const candidate = value as Partial<ApiErrorResponse>;
  return typeof candidate.message === "string" && typeof candidate.status === "number";
}

export function parseApiError(error: unknown): ApiClientError {
  if (error instanceof ApiClientError) return error;

  if (axios.isAxiosError(error)) {
    if (isApiErrorResponse(error.response?.data)) {
      const response = error.response.data;
      return new ApiClientError(response.message, {
        status: response.status,
        code: response.code,
        path: response.path,
        validationErrors: response.validationErrors,
      });
    }

    if (!error.response) {
      return new ApiClientError("تعذر الاتصال بالخادم. تحقق من الشبكة ثم حاول مرة أخرى.", {
        code: "NETWORK_ERROR",
      });
    }
  }

  if (error instanceof Error && error.message) {
    return new ApiClientError(error.message);
  }

  return new ApiClientError("حصلت مشكلة، جرّب تاني");
}

export function getFieldError(error: unknown, field: string): string | undefined {
  return parseApiError(error).validationErrors.find((item) => item.field === field)?.message;
}

const arabicMessages: Record<string, string> = {
  "Financial records cannot be changed for a closed business date.": "لا يمكن تعديل السجلات المالية في تاريخ عمل مُقفل. يجب أن يعيد المالك فتح اليوم أولًا.",
  "Receptionists cannot cancel old expenses.": "لا يملك موظف الاستقبال صلاحية إلغاء مصروف قديم.",
  "The expense is already cancelled.": "هذا المصروف ملغي بالفعل.",
  "The employee transaction is already cancelled.": "هذه الحركة المالية ملغاة بالفعل.",
  "Transactions can only be created for an active employee.": "لا يمكن تسجيل حركة مالية إلا لموظف نشط.",
  "Attendance can only be recorded for an active employee.": "لا يمكن تسجيل الحضور إلا لموظف نشط.",
  "Set a positive daily wage for the employee before marking attendance present.": "حدد أجرًا يوميًا أكبر من صفر قبل تسجيل الحضور.",
  "Daily wage is already managed by the employee attendance record for this date.": "أجر هذا اليوم مسجل بالفعل من صفحة الحضور.",
  "This attendance wage may already be settled. Correct the employee payment before marking attendance absent.": "قد يكون أجر هذا الحضور قد سُوّي بالفعل. صحح دفعة الموظف أولًا قبل تسجيله غائبًا.",
  "More than one active daily wage exists for this employee and date. Correct the existing financial entries first.": "يوجد أكثر من أجر يومي نشط لهذا الموظف في التاريخ نفسه. صحح الحركات المالية الحالية أولًا.",
  "Inactive expense categories cannot be used for new expenses.": "لا يمكن استخدام تصنيف مصروفات غير نشط.",
  "Inactive payment methods cannot be used for new records.": "لا يمكن استخدام طريقة دفع غير نشطة في سجل جديد.",
  "A payment reference is required for the selected payment method.": "رقم مرجع الدفع مطلوب لطريقة الدفع المحددة.",
  "Amount must be greater than zero.": "يجب أن يكون المبلغ أكبر من صفر.",
  "Amount must have at most 2 decimal digits.": "يجب ألا يحتوي المبلغ على أكثر من منزلتين عشريتين.",
  "fromDate must be on or before toDate.": "يجب أن يكون تاريخ البداية سابقًا لتاريخ النهاية أو مساويًا له.",
  "Both fromDate and toDate are required.": "تاريخا البداية والنهاية مطلوبان.",
  "Employee not found.": "الموظف غير موجود.",
  "Employee transaction not found.": "الحركة المالية غير موجودة.",
  "Expense not found.": "المصروف غير موجود.",
  "Expense category not found.": "تصنيف المصروف غير موجود.",
  "Payment method not found.": "طريقة الدفع غير موجودة.",
  "An expense category with one of these names already exists.": "يوجد تصنيف مصروفات بالاسم العربي أو الإنجليزي نفسه.",
  "A payment method with this code already exists.": "يوجد بالفعل طريقة دفع بهذا الرمز.",
  "Receipt path must be a managed relative path without parent traversal.": "يجب أن يكون مسار الإيصال نسبيًا وآمنًا داخل مجلد الإيصالات.",
  "A catalog category with one of these names already exists.": "يوجد تصنيف كتالوج بالاسم العربي أو الإنجليزي نفسه.",
  "A catalog item with this code already exists.": "يوجد بالفعل صنف بهذا الرمز.",
  "Inactive catalog categories cannot be used for new or updated records.": "لا يمكن استخدام تصنيف كتالوج غير نشط في سجل جديد أو محدث.",
  "Inactive catalog items or categories cannot be used for new records.": "لا يمكن استخدام صنف أو تصنيف غير نشط في سجل جديد.",
  "At least one price or cost value must change.": "يجب تغيير سعر البيع أو سعر الشراء على الأقل.",
  "fromDateTime must be on or before toDateTime.": "يجب أن يسبق وقت البداية وقت النهاية أو يساويه.",
  "A discount may target an item or a category, but not both.": "يمكن ربط الخصم بصنف أو تصنيف واحد فقط، وليس كليهما.",
  "Start date must be on or before end date.": "يجب ألا يسبق تاريخ النهاية تاريخ البداية.",
  "Percentage discounts must be between 0 and 100.": "يجب أن تكون نسبة الخصم بين 0 و100.",
  "Inactive, not-yet-active, or expired discounts cannot be applied.": "لا يمكن تطبيق خصم غير نشط أو لم يبدأ أو منتهي الصلاحية.",
  "A fixed discount cannot create a negative final price.": "قيمة الخصم الثابت أكبر من قيمة البند ولا يمكن أن تجعل السعر سالبًا.",
  "The discount does not apply to this catalog item.": "الخصم المحدد لا ينطبق على هذا الصنف.",
  "The discount does not apply to this catalog category.": "الخصم المحدد لا ينطبق على تصنيف هذا الصنف.",
  "Inactive hair configurations cannot be calculated.": "لا يمكن حساب إعداد شعر غير نشط.",
  "A hair configuration cannot be used before its effective date.": "لا يمكن استخدام إعداد الشعر قبل تاريخ سريانه.",
  "Quantity is below the configuration minimum.": "الكمية أقل من الحد الأدنى لإعداد الشعر.",
  "Quantity is below the hair configuration minimum.": "الكمية أقل من الحد الأدنى لإعداد الشعر.",
  "The hair configuration does not belong to the selected catalog item.": "إعداد الشعر لا يخص صنف الكتالوج المحدد.",
  "Override amount must differ from the calculated final amount.": "يجب أن يختلف السعر اليدوي عن السعر النهائي المحسوب.",
  "Price override amount must differ from the calculated final amount.": "يجب أن يختلف سعر البند اليدوي عن السعر المحسوب.",
  "Override amount is required when an override reason is supplied.": "السعر اليدوي مطلوب عند إدخال سبب التعديل.",
  "Price override amount is required when a reason is supplied.": "سعر البند اليدوي مطلوب عند إدخال سبب التعديل.",
  "Only active employees may be assigned to new invoice items.": "يمكن إسناد بند الفاتورة إلى موظف نشط فقط.",
  "An invoice must contain at least one item.": "يجب أن تحتوي الفاتورة على بند واحد على الأقل.",
  "Payment total must equal the final invoice amount.": "يجب أن يساوي مجموع الدفعات إجمالي الفاتورة النهائي.",
  "A payment method may appear only once in a payment split.": "لا يمكن تكرار طريقة الدفع في توزيع الفاتورة نفسه.",
  "A reference number is required for the selected payment method.": "الرقم المرجعي مطلوب لطريقة الدفع المحددة.",
  "Only draft invoices may be edited.": "يمكن تعديل الفواتير المسودة فقط.",
  "The invoice is already cancelled.": "الفاتورة ملغاة بالفعل.",
  "Refunded invoices cannot be cancelled.": "لا يمكن إلغاء فاتورة تم رد قيمتها.",
  "Draft item discount data changed; update the item before completion.": "تغيرت بيانات خصم أحد بنود المسودة؛ أعد إضافة البند قبل الإكمال.",
  "Catalog item not found.": "صنف الكتالوج غير موجود.",
  "Catalog category not found.": "تصنيف الكتالوج غير موجود.",
  "Discount not found.": "الخصم غير موجود.",
  "Hair configuration not found.": "إعداد الشعر غير موجود.",
  "Invoice not found.": "الفاتورة غير موجودة.",
  "Invoice item not found.": "بند الفاتورة غير موجود.",
  "The business date is already closed.": "تاريخ العمل مغلق بالفعل.",
  "Daily closing not found.": "لا توجد لقطة إغلاق محفوظة لهذا التاريخ.",
  "Actual cash must be zero or greater.": "يجب أن تكون النقدية الفعلية صفرًا أو أكبر.",
  "Actual cash must have at most 2 decimal digits.": "يجب ألا تحتوي النقدية الفعلية على أكثر من منزلتين عشريتين.",
  "Reopening reason is required.": "سبب إعادة فتح اليوم مطلوب.",
  "Database backup is disabled.": "النسخ الاحتياطي اليدوي غير مفعّل على الخادم.",
  "A database backup is already running.": "توجد عملية نسخ احتياطي قيد التنفيذ بالفعل.",
  "Database backup failed. Check server logs and backup configuration.": "فشل النسخ الاحتياطي. راجع إعدادات المجلد وpg_dump وسجل الخادم.",
  "Access Denied": "ليست لديك صلاحية لتنفيذ هذا الإجراء.",
};

export function translateServerMessage(message: string): string {
  if (/[\u0600-\u06FF]/.test(message)) return message;
  const exact = arabicMessages[message];
  if (exact) return exact;
  if (message.includes("is required")) return "يرجى استكمال الحقول المطلوبة ثم المحاولة مرة أخرى.";
  if (message.includes("must not exceed")) return "تجاوزت إحدى القيم الحد الأقصى المسموح به.";
  if (message.includes("must contain 1 to")) return "إحدى القيم النصية فارغة أو تتجاوز الطول المسموح به.";
  if (message.includes("not found")) return "تعذر العثور على السجل المطلوب.";
  return "حصلت مشكلة، جرّب تاني";
}

export function getArabicApiErrorMessage(error: unknown): string {
  const parsed = parseApiError(error);
  if (parsed.validationErrors.length > 0) {
    return parsed.validationErrors.map((item) => translateServerMessage(item.message)).join(" ");
  }
  return translateServerMessage(parsed.message);
}

export function isClosedDateError(error: unknown): boolean {
  return parseApiError(error).message === "Financial records cannot be changed for a closed business date.";
}
