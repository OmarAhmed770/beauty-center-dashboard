import type { MoneyValue } from "@/lib/format";
import type { PageResponse } from "@/types/api";
import type { PaymentMethodType } from "@/features/payment-methods/types";
import type { SystemPermission } from "@/types/auth";

export type EmployeeTransactionType = "DAILY_WAGE" | "ADVANCE" | "BONUS" | "DEDUCTION" | "COMMISSION" | "PAYMENT";
export type EmployeeTransactionStatus = "ACTIVE" | "CANCELLED";
export type EmployeeAttendanceStatus = "PRESENT" | "ABSENT";

export interface Employee {
  id: number;
  name: string;
  phone: string | null;
  position: string;
  defaultDailyWage: MoneyValue;
  active: boolean;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface EmployeeInput {
  name: string;
  phone?: string;
  position: string;
  defaultDailyWage: string;
  notes?: string;
}

export interface EmployeeTransaction {
  id: number;
  employeeId: number;
  employeeName: string;
  type: EmployeeTransactionType;
  amount: MoneyValue;
  businessDate: string;
  paymentMethodId: number | null;
  paymentMethod: string | null;
  paymentMethodType: PaymentMethodType | null;
  notes: string | null;
  status: EmployeeTransactionStatus;
  cancelledAt: string | null;
  cancelledBy: string | null;
  cancellationReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface EmployeeTransactionInput {
  employeeId: number;
  type: EmployeeTransactionType;
  amount: string;
  businessDate: string;
  paymentMethod?: string;
  notes?: string;
}

export interface EmployeeTotals {
  employeeId: number;
  employeeName: string;
  fromDate: string;
  toDate: string;
  dailyWages: MoneyValue;
  bonuses: MoneyValue;
  commissions: MoneyValue;
  deductions: MoneyValue;
  advances: MoneyValue;
  payments: MoneyValue;
  netEntitlement: MoneyValue;
}

export interface EmployeeStatement {
  employee: Employee;
  fromDate: string;
  toDate: string;
  totals: EmployeeTotals;
  transactions: PageResponse<EmployeeTransaction>;
}

export interface EmployeeAttendance {
  id: number | null;
  employeeId: number;
  employeeName: string;
  employeeActive: boolean;
  businessDate: string;
  status: EmployeeAttendanceStatus | null;
  dailyWageAmount: MoneyValue;
  dailyWageTransactionId: number | null;
  dailyWageTransactionStatus: EmployeeTransactionStatus | null;
  version: number;
  createdAt: string | null;
  updatedAt: string | null;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface EmployeePermissions {
  employeeId: number;
  userId: number | null;
  username: string | null;
  accountLinked: boolean;
  permissions: SystemPermission[];
}
