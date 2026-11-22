import type { ExpenseCategory } from "@/features/expense-categories/types";
import type { PaymentMethodType } from "@/features/payment-methods/types";
import type { MoneyValue } from "@/lib/format";

export type ExpenseStatus = "ACTIVE" | "CANCELLED";

export interface Expense {
  id: number;
  categoryId: number;
  categoryNameArabic: string;
  categoryNameEnglish: string;
  amount: MoneyValue;
  businessDate: string;
  paymentMethodId: number;
  paymentMethodCode: string;
  paymentMethodNameArabic: string;
  paymentMethodNameEnglish: string;
  paymentMethodType: PaymentMethodType;
  paymentReference: string | null;
  description: string;
  notes: string | null;
  receiptPath: string | null;
  status: ExpenseStatus;
  cancelledAt: string | null;
  cancelledBy: string | null;
  cancellationReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface ExpenseInput {
  categoryId: number;
  amount: string;
  businessDate: string;
  paymentMethodId: number;
  description: string;
  notes?: string;
}

export interface ExpenseSummary {
  fromDate: string;
  toDate: string;
  activeExpenseCount: number;
  cancelledExpenseCount: number;
  totalAmount: MoneyValue;
  cashAmount: MoneyValue;
  nonCashAmount: MoneyValue;
}

export interface ExpenseCategoryTotal {
  categoryId: number;
  nameArabic: string;
  nameEnglish: string;
  totalAmount: MoneyValue;
  expenseCount: number;
}

export interface ExpensePaymentMethodTotal {
  paymentMethodId: number;
  code: string;
  displayNameArabic: string;
  displayNameEnglish: string;
  type: PaymentMethodType;
  totalAmount: MoneyValue;
  expenseCount: number;
}

export type ActiveExpenseCategory = Pick<ExpenseCategory, "id" | "nameArabic" | "active">;
