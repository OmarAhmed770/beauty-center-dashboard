import type { CatalogItemType } from "@/features/catalog/types";
import type { PaymentMethodType } from "@/features/payment-methods/types";
import type { MoneyValue } from "@/lib/format";

export interface PaymentFinancialTotal {
  paymentMethodId: number | null;
  code: string;
  displayNameArabic: string;
  displayNameEnglish: string;
  type: PaymentMethodType;
  receivedAmount: MoneyValue;
  refundedAmount: MoneyValue;
  netAmount: MoneyValue;
}

export interface ExpenseCategoryFinancialTotal {
  categoryId: number | null;
  nameArabic: string;
  nameEnglish: string;
  amount: MoneyValue;
  expenseCount: number;
}

export interface TopCatalogItem {
  catalogItemId: number | null;
  itemCode: string;
  itemNameArabic: string;
  itemNameEnglish: string;
  itemType: CatalogItemType;
  quantity: MoneyValue;
  netRevenue: MoneyValue;
  invoiceCount: number;
}

export interface TopEmployee {
  employeeId: number | null;
  employeeName: string;
  quantity: MoneyValue;
  netRevenue: MoneyValue;
  invoiceCount: number;
}

export interface DailySalesTrend {
  businessDate: string;
  grossSales: MoneyValue;
  discounts: MoneyValue;
  refunds: MoneyValue;
  netSales: MoneyValue;
  completedInvoiceCount: number;
}

export interface FinancialSummary {
  fromDate: string;
  toDate: string;
  grossSales: MoneyValue;
  discounts: MoneyValue;
  refunds: MoneyValue;
  netSales: MoneyValue;
  completedInvoiceCount: number;
  expenses: MoneyValue;
  netSalesAfterExpenses: MoneyValue;
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
  expectedCashBalance: MoneyValue;
  paymentTotalsByMethod: PaymentFinancialTotal[];
  expenseTotalsByCategory: ExpenseCategoryFinancialTotal[];
  topServicesAndProducts: TopCatalogItem[];
  topPerformingEmployees: TopEmployee[];
  dailySalesTrend: DailySalesTrend[];
}

export interface ProfitReport {
  fromDate: string;
  toDate: string;
  netSales: MoneyValue;
  costOfSoldItemsAndHair: MoneyValue;
  operatingExpenses: MoneyValue;
  employeeCost: MoneyValue;
  netProfit: MoneyValue;
}
