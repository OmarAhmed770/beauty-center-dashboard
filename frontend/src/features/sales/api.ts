import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { InvoiceDetails, InvoiceItemInput, InvoicePaymentInput, InvoiceStatus, InvoiceSummary, InvoiceTotalsPreview, QuickOrderInput, QuickOrderResponse, SimpleOrderInput } from "@/features/sales/types";

export const invoiceKeys = {
  all: ["invoices"] as const,
  list: (params?: object) => ["invoices", "list", params ?? {}] as const,
  detail: (id: number) => ["invoices", "detail", id] as const,
  preview: (id: number) => ["invoices", "preview", id] as const,
};

export async function createDraftInvoice(input: { businessDate: string; customerName?: string; customerPhone?: string; notes?: string }): Promise<InvoiceDetails> {
  const { data } = await apiClient.post<InvoiceDetails>("/api/v1/invoices", input);
  return data;
}

export async function createSimpleOrder(input: SimpleOrderInput): Promise<InvoiceDetails> {
  const { data } = await apiClient.post<InvoiceDetails>("/api/v1/invoices/simple-orders", input);
  return data;
}

export async function createQuickOrder(input: QuickOrderInput): Promise<QuickOrderResponse> {
  const { data } = await apiClient.post<QuickOrderResponse>("/api/v1/invoices/quick-orders", input);
  return data;
}

export async function addInvoiceItem(invoiceId: number, input: InvoiceItemInput): Promise<InvoiceDetails> {
  const { data } = await apiClient.post<InvoiceDetails>(`/api/v1/invoices/${invoiceId}/items`, input);
  return data;
}

export async function updateInvoiceItem(invoiceId: number, itemId: number, input: InvoiceItemInput): Promise<InvoiceDetails> {
  const { data } = await apiClient.put<InvoiceDetails>(`/api/v1/invoices/${invoiceId}/items/${itemId}`, input);
  return data;
}

export async function removeInvoiceItem(invoiceId: number, itemId: number): Promise<InvoiceDetails> {
  const { data } = await apiClient.delete<InvoiceDetails>(`/api/v1/invoices/${invoiceId}/items/${itemId}`);
  return data;
}

export async function setInvoicePayments(invoiceId: number, payments: InvoicePaymentInput[]): Promise<InvoiceDetails> {
  const { data } = await apiClient.put<InvoiceDetails>(`/api/v1/invoices/${invoiceId}/payments`, { payments });
  return data;
}

export async function previewInvoiceTotals(invoiceId: number): Promise<InvoiceTotalsPreview> {
  const { data } = await apiClient.get<InvoiceTotalsPreview>(`/api/v1/invoices/${invoiceId}/preview`);
  return data;
}

export async function completeInvoice(invoiceId: number): Promise<InvoiceDetails> {
  const { data } = await apiClient.post<InvoiceDetails>(`/api/v1/invoices/${invoiceId}/complete`);
  return data;
}

export async function getInvoice(id: number): Promise<InvoiceDetails> {
  const { data } = await apiClient.get<InvoiceDetails>(`/api/v1/invoices/${id}`);
  return data;
}

export async function searchInvoices(params: { status?: InvoiceStatus; fromDate?: string; toDate?: string; query?: string; page?: number; size?: number } = {}): Promise<PageResponse<InvoiceSummary>> {
  const { data } = await apiClient.get<PageResponse<InvoiceSummary>>("/api/v1/invoices", { params });
  return data;
}

export async function cancelInvoice(id: number, reason: string): Promise<InvoiceDetails> {
  const { data } = await apiClient.post<InvoiceDetails>(`/api/v1/invoices/${id}/cancel`, { reason });
  return data;
}
