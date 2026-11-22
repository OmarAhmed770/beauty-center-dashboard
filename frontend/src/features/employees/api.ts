import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type { Employee, EmployeeAttendance, EmployeeAttendanceStatus, EmployeeInput, EmployeePermissions, EmployeeStatement, EmployeeTransaction, EmployeeTransactionInput, EmployeeTransactionStatus, EmployeeTransactionType } from "@/features/employees/types";
import type { SystemPermission } from "@/types/auth";

export const employeeKeys = {
  all: ["employees"] as const,
  list: (params: object) => ["employees", "list", params] as const,
  detail: (id: number) => ["employees", "detail", id] as const,
  statement: (id: number, params: object) => ["employees", "statement", id, params] as const,
  attendance: (businessDate: string) => ["employees", "attendance", businessDate] as const,
  permissions: (id: number) => ["employees", id, "permissions"] as const,
};

export async function searchEmployees(params: { query?: string; active?: boolean; page?: number; size?: number }): Promise<PageResponse<Employee>> {
  const { data } = await apiClient.get<PageResponse<Employee>>("/api/v1/employees", { params });
  return data;
}

export async function getEmployee(id: number): Promise<Employee> {
  const { data } = await apiClient.get<Employee>(`/api/v1/employees/${id}`);
  return data;
}

export async function createEmployee(input: EmployeeInput): Promise<Employee> {
  const { data } = await apiClient.post<Employee>("/api/v1/employees", input);
  return data;
}

export async function updateEmployee(id: number, input: EmployeeInput): Promise<Employee> {
  const { data } = await apiClient.put<Employee>(`/api/v1/employees/${id}`, input);
  return data;
}

export async function setEmployeeActive(id: number, active: boolean): Promise<Employee> {
  const { data } = await apiClient.patch<Employee>(`/api/v1/employees/${id}/active`, { active });
  return data;
}

export async function createEmployeeTransaction(input: EmployeeTransactionInput): Promise<EmployeeTransaction> {
  const { data } = await apiClient.post<EmployeeTransaction>("/api/v1/employee-transactions", input);
  return data;
}

export async function cancelEmployeeTransaction(id: number, reason: string): Promise<EmployeeTransaction> {
  const { data } = await apiClient.post<EmployeeTransaction>(`/api/v1/employee-transactions/${id}/cancel`, { reason });
  return data;
}

export async function searchEmployeeTransactions(params: { employeeId?: number; type?: EmployeeTransactionType; status?: EmployeeTransactionStatus; fromDate?: string; toDate?: string; page?: number; size?: number }): Promise<PageResponse<EmployeeTransaction>> {
  const { data } = await apiClient.get<PageResponse<EmployeeTransaction>>("/api/v1/employee-transactions", { params });
  return data;
}

export async function getEmployeeStatement(id: number, params: { fromDate: string; toDate: string; page: number; size: number }): Promise<EmployeeStatement> {
  const { data } = await apiClient.get<EmployeeStatement>(`/api/v1/employees/${id}/statement`, { params });
  return data;
}

export async function getEmployeeAttendance(businessDate: string): Promise<EmployeeAttendance[]> {
  const { data } = await apiClient.get<EmployeeAttendance[]>("/api/v1/employees/attendance", { params: { businessDate } });
  return data;
}

export async function saveEmployeeAttendance(
  employeeId: number,
  businessDate: string,
  status: EmployeeAttendanceStatus,
): Promise<EmployeeAttendance> {
  const { data } = await apiClient.put<EmployeeAttendance>(`/api/v1/employees/${employeeId}/attendance`, { status }, { params: { businessDate } });
  return data;
}

export async function getEmployeePermissions(id: number): Promise<EmployeePermissions> {
  const { data } = await apiClient.get<EmployeePermissions>(`/api/v1/employees/${id}/permissions`);
  return data;
}

export async function updateEmployeePermissions(id: number, permissions: SystemPermission[]): Promise<EmployeePermissions> {
  const { data } = await apiClient.put<EmployeePermissions>(`/api/v1/employees/${id}/permissions`, { permissions });
  return data;
}
