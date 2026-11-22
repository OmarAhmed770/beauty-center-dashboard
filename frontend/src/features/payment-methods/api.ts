import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { PaymentMethod, PaymentMethodInput, PaymentMethodType } from "@/features/payment-methods/types";

export const paymentMethodKeys = { all: ["payment-methods"] as const };

export async function searchPaymentMethods(params: { query?: string; active?: boolean; type?: PaymentMethodType; page?: number; size?: number } = {}): Promise<PageResponse<PaymentMethod>> {
  const { data } = await apiClient.get<PageResponse<PaymentMethod>>("/api/v1/payment-methods", { params });
  return data;
}

export async function createPaymentMethod(input: PaymentMethodInput): Promise<PaymentMethod> {
  const { data } = await apiClient.post<PaymentMethod>("/api/v1/payment-methods", input);
  return data;
}

export async function updatePaymentMethod(id: number, input: PaymentMethodInput): Promise<PaymentMethod> {
  const { code: _code, active: _active, ...request } = input;
  const { data } = await apiClient.put<PaymentMethod>(`/api/v1/payment-methods/${id}`, request);
  return data;
}

export async function setPaymentMethodActive(id: number, active: boolean): Promise<PaymentMethod> {
  const { data } = await apiClient.patch<PaymentMethod>(`/api/v1/payment-methods/${id}/active`, { active });
  return data;
}
