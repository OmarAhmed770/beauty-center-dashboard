import type { PriceHistory, PriceValueType } from "@/features/catalog/types";
import type { EmployeeStatement } from "@/features/employees/types";
import type { Expense, ExpenseStatus } from "@/features/expenses/types";
import type { InvoiceStatus, InvoiceSummary } from "@/features/sales/types";
import type { DailyClosing } from "@/features/closing/types";
import type { FinancialSummary, PaymentFinancialTotal, ProfitReport } from "@/features/reports/types";
import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";

export const reportKeys = {
  all: ["reports"] as const,
  dashboard: (params: object) => ["reports", "dashboard", params] as const,
  financial: (params: object) => ["reports", "financial", params] as const,
  detail: (name: string, params: object) => ["reports", name, params] as const,
};

export async function getDashboard(params: { filter: string; fromDate?: string; toDate?: string }): Promise<FinancialSummary> {
  const { data } = await apiClient.get<FinancialSummary>("/api/v1/dashboard", { params });
  return data;
}

export async function getFinancialReport(params: { fromDate: string; toDate: string }): Promise<FinancialSummary> {
  const { data } = await apiClient.get<FinancialSummary>("/api/v1/reports/financial", { params });
  return data;
}

export async function getDailyClosingReport(businessDate: string): Promise<DailyClosing> {
  const { data } = await apiClient.get<DailyClosing>(`/api/v1/reports/daily-closing/${businessDate}`);
  return data;
}

export async function getEmployeeStatementReport(employeeId: number, params: { fromDate: string; toDate: string; page?: number; size?: number }): Promise<EmployeeStatement> {
  const { data } = await apiClient.get<EmployeeStatement>(`/api/v1/reports/employees/${employeeId}/statement`, { params });
  return data;
}

export async function getExpenseReport(params: { categoryId?: number; paymentMethodId?: number; status?: ExpenseStatus; fromDate: string; toDate: string; query?: string; page?: number; size?: number }): Promise<PageResponse<Expense>> {
  const { data } = await apiClient.get<PageResponse<Expense>>("/api/v1/reports/expenses", { params });
  return data;
}

export async function getSalesReport(params: { status?: InvoiceStatus; fromDate: string; toDate: string; query?: string; page?: number; size?: number }): Promise<PageResponse<InvoiceSummary>> {
  const { data } = await apiClient.get<PageResponse<InvoiceSummary>>("/api/v1/reports/sales", { params });
  return data;
}

export async function getPaymentMethodReport(params: { fromDate: string; toDate: string }): Promise<PaymentFinancialTotal[]> {
  const { data } = await apiClient.get<PaymentFinancialTotal[]>("/api/v1/reports/payment-methods", { params });
  return data;
}

export async function getProfitReport(params: { fromDate: string; toDate: string }): Promise<ProfitReport> {
  const { data } = await apiClient.get<ProfitReport>("/api/v1/reports/profit", { params });
  return data;
}

export async function getPriceHistoryReport(params: { itemId?: number; valueType?: PriceValueType; fromDateTime?: string; toDateTime?: string; page?: number; size?: number }): Promise<PageResponse<PriceHistory>> {
  const { data } = await apiClient.get<PageResponse<PriceHistory>>("/api/v1/reports/price-history", { params });
  return data;
}
