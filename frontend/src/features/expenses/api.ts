import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { Expense, ExpenseCategoryTotal, ExpenseInput, ExpensePaymentMethodTotal, ExpenseStatus, ExpenseSummary } from "@/features/expenses/types";

export const expenseKeys = {
  all: ["expenses"] as const,
  list: (params: object) => ["expenses", "list", params] as const,
  summary: (params: object) => ["expenses", "summary", params] as const,
  byCategory: (params: object) => ["expenses", "summary", "category", params] as const,
  byPayment: (params: object) => ["expenses", "summary", "payment", params] as const,
};

export async function searchExpenses(params: { categoryId?: number; paymentMethodId?: number; status?: ExpenseStatus; fromDate?: string; toDate?: string; query?: string; page?: number; size?: number }): Promise<PageResponse<Expense>> {
  const { data } = await apiClient.get<PageResponse<Expense>>("/api/v1/expenses", { params });
  return data;
}

export async function createExpense(input: ExpenseInput): Promise<Expense> {
  const { data } = await apiClient.post<Expense>("/api/v1/expenses", input);
  return data;
}

export async function cancelExpense(id: number, reason: string): Promise<Expense> {
  const { data } = await apiClient.post<Expense>(`/api/v1/expenses/${id}/cancel`, { reason });
  return data;
}

export async function getExpenseSummary(params: { fromDate: string; toDate: string }): Promise<ExpenseSummary> {
  const { data } = await apiClient.get<ExpenseSummary>("/api/v1/expenses/summary", { params });
  return data;
}

export async function getExpenseTotalsByCategory(params: { fromDate: string; toDate: string }): Promise<ExpenseCategoryTotal[]> {
  const { data } = await apiClient.get<ExpenseCategoryTotal[]>("/api/v1/expenses/summary/by-category", { params });
  return data;
}

export async function getExpenseTotalsByPaymentMethod(params: { fromDate: string; toDate: string }): Promise<ExpensePaymentMethodTotal[]> {
  const { data } = await apiClient.get<ExpensePaymentMethodTotal[]>("/api/v1/expenses/summary/by-payment-method", { params });
  return data;
}
