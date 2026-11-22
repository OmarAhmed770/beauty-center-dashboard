import type { ExpenseCategoryFinancialTotal, FinancialSummary, PaymentFinancialTotal } from "@/features/reports/types";
import type { MoneyValue } from "@/lib/format";

export type DailyClosingStatus = "OPEN" | "CLOSED" | "REOPENED";

export interface DailyClosingPreview {
  businessDate: string;
  status: DailyClosingStatus;
  liveFinancials: FinancialSummary;
}

export interface DailyClosing {
  id: number;
  businessDate: string;
  closingVersion: number;
  status: DailyClosingStatus;
  grossSales: MoneyValue;
  discounts: MoneyValue;
  refunds: MoneyValue;
  netSales: MoneyValue;
  completedInvoiceCount: number;
  expenseTotal: MoneyValue;
  dailyWages: MoneyValue;
  bonuses: MoneyValue;
  commissions: MoneyValue;
  employeeCost: MoneyValue;
  advances: MoneyValue;
  employeePayments: MoneyValue;
  costOfSoldItemsAndHair: MoneyValue;
  netProfit: MoneyValue;
  cashPaymentsReceived: MoneyValue;
  cashExpenses: MoneyValue;
  cashEmployeePayments: MoneyValue;
  cashRefunds: MoneyValue;
  cashAdjustments: MoneyValue;
  expectedCash: MoneyValue;
  actualCash: MoneyValue;
  cashDifference: MoneyValue;
  paymentMethodBreakdown: PaymentFinancialTotal[];
  expenseCategoryBreakdown: ExpenseCategoryFinancialTotal[];
  closedBy: string;
  closedAt: string;
  notes: string | null;
  reopenedBy: string | null;
  reopenedAt: string | null;
  reopeningReason: string | null;
  reopenCount: number;
  createdAt: string;
  updatedAt: string;
}
