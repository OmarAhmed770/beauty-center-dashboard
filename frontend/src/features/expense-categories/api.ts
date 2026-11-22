import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { ExpenseCategory, ExpenseCategoryInput } from "@/features/expense-categories/types";

export const expenseCategoryKeys = { all: ["expense-categories"] as const };

export async function searchExpenseCategories(params: { query?: string; active?: boolean; page?: number; size?: number } = {}): Promise<PageResponse<ExpenseCategory>> {
  const { data } = await apiClient.get<PageResponse<ExpenseCategory>>("/api/v1/expense-categories", { params });
  return data;
}

export async function createExpenseCategory(input: ExpenseCategoryInput): Promise<ExpenseCategory> {
  const { data } = await apiClient.post<ExpenseCategory>("/api/v1/expense-categories", input);
  return data;
}

export async function updateExpenseCategory(id: number, input: ExpenseCategoryInput): Promise<ExpenseCategory> {
  const { active: _active, ...request } = input;
  const { data } = await apiClient.put<ExpenseCategory>(`/api/v1/expense-categories/${id}`, request);
  return data;
}

export async function setExpenseCategoryActive(id: number, active: boolean): Promise<ExpenseCategory> {
  const { data } = await apiClient.patch<ExpenseCategory>(`/api/v1/expense-categories/${id}/active`, { active });
  return data;
}
