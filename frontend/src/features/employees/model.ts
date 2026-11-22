import { z } from "zod";
import type { EmployeeTransactionType } from "@/features/employees/types";
import type { UserRole } from "@/types/auth";

const moneyPattern = /^\d{1,17}(\.\d{1,2})?$/;

export const employeeSchema = z.object({
  name: z.string().trim().min(1, "اسم الموظف مطلوب").max(150, "يجب ألا يتجاوز الاسم 150 حرفًا"),
  phone: z.string().trim().max(50, "يجب ألا يتجاوز الهاتف 50 حرفًا"),
  position: z.string().trim().min(1, "المسمى الوظيفي مطلوب").max(100, "يجب ألا يتجاوز المسمى 100 حرف"),
  defaultDailyWage: z.string().trim().min(1, "الأجر اليومي مطلوب").regex(moneyPattern, "أدخل مبلغًا صحيحًا بمنزلتين عشريتين كحد أقصى"),
  notes: z.string().trim().max(2000, "يجب ألا تتجاوز الملاحظات 2000 حرف"),
});

export type EmployeeFormValues = z.infer<typeof employeeSchema>;

export const employeeTransactionSchema = z.object({
  type: z.enum(["DAILY_WAGE", "ADVANCE", "BONUS", "DEDUCTION", "COMMISSION", "PAYMENT"]),
  amount: z.string().trim().regex(moneyPattern, "أدخل مبلغًا صحيحًا بمنزلتين عشريتين كحد أقصى").refine((value) => Number(value) > 0, "يجب أن يكون المبلغ أكبر من صفر"),
  businessDate: z.string().min(1, "تاريخ العمل مطلوب"),
  paymentMethod: z.string(),
  notes: z.string().trim().max(2000, "يجب ألا تتجاوز الملاحظات 2000 حرف"),
});

export type EmployeeTransactionFormValues = z.infer<typeof employeeTransactionSchema>;

export const employeeTransactionTypeLabels: Record<EmployeeTransactionType, string> = {
  DAILY_WAGE: "أجر يومي",
  ADVANCE: "سلفة",
  BONUS: "مكافأة",
  DEDUCTION: "خصم",
  COMMISSION: "عمولة",
  PAYMENT: "دفعة للموظف",
};

export const employeeTransactionTypes = Object.keys(employeeTransactionTypeLabels) as EmployeeTransactionType[];
const receptionistTransactionTypes: EmployeeTransactionType[] = ["DAILY_WAGE"];

export function availableEmployeeTransactionTypes(roles: readonly UserRole[], canManageEmployees = false): EmployeeTransactionType[] {
  return roles.includes("OWNER") || canManageEmployees ? employeeTransactionTypes : receptionistTransactionTypes;
}
