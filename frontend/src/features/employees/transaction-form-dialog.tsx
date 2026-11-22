import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { MoneyInput } from "@/components/shared/money-input";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/auth/auth-context";
import { createEmployeeTransaction, employeeKeys } from "@/features/employees/api";
import { availableEmployeeTransactionTypes, employeeTransactionSchema, employeeTransactionTypeLabels, type EmployeeTransactionFormValues } from "@/features/employees/model";
import type { Employee, EmployeeTransaction, EmployeeTransactionType } from "@/features/employees/types";
import { paymentMethodKeys, searchPaymentMethods } from "@/features/payment-methods/api";
import { cairoDateIso } from "@/components/shared/date-range-filter";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { invalidateFinancialViews } from "@/lib/financial-query-invalidation";

export function TransactionFormDialog({ open, employee, onOpenChange, onSaved, initialType }: { open: boolean; employee: Employee; onOpenChange: (open: boolean) => void; onSaved?: (transaction: EmployeeTransaction) => void; initialType?: EmployeeTransactionType }) {
  const { user, hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const allowedTypes = availableEmployeeTransactionTypes(user?.roles ?? [], hasPermission("EMPLOYEES"));
  const paymentMethodsQuery = useQuery({
    queryKey: [...paymentMethodKeys.all, "active"],
    queryFn: () => searchPaymentMethods({ active: true, page: 0, size: 100 }),
    enabled: open,
    staleTime: 5 * 60_000,
  });
  const { register, handleSubmit, reset, setError, watch, setValue, formState: { errors } } = useForm<EmployeeTransactionFormValues>({
    resolver: zodResolver(employeeTransactionSchema),
    defaultValues: { type: allowedTypes[0], amount: "", businessDate: cairoDateIso(), paymentMethod: "", notes: "" },
  });
  const selectedType = watch("type");
  const defaultType = initialType && allowedTypes.includes(initialType) ? initialType : allowedTypes[0];

  useEffect(() => {
    if (!open) return;
    reset({ type: defaultType, amount: defaultType === "DAILY_WAGE" ? String(employee.defaultDailyWage) : "", businessDate: cairoDateIso(), paymentMethod: "", notes: "" });
  }, [defaultType, employee.defaultDailyWage, open, reset]);

  useEffect(() => {
    if (selectedType === "DAILY_WAGE") setValue("amount", String(employee.defaultDailyWage), { shouldValidate: true });
  }, [employee.defaultDailyWage, selectedType, setValue]);

  const mutation = useMutation({
    mutationFn: (values: EmployeeTransactionFormValues) => createEmployeeTransaction({ employeeId: employee.id, ...values, paymentMethod: values.paymentMethod || undefined }),
    onSuccess: async (saved) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: employeeKeys.all }),
        invalidateFinancialViews(queryClient),
      ]);
      onSaved?.(saved);
      onOpenChange(false);
    },
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });

  return (
    <DialogShell
      open={open}
      title={`تسجيل حركة مالية — ${employee.name}`}
      description={allowedTypes.length === 1 ? "صلاحية الاستقبال تسمح بتسجيل الأجر اليومي فقط." : "اختر نوع الحركة وتاريخها وطريقة الدفع عند الحاجة."}
      onOpenChange={onOpenChange}
      size="lg"
      footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button type="submit" form="employee-transaction-form" disabled={mutation.isPending || !employee.active}>{mutation.isPending ? "جارٍ التسجيل..." : "تسجيل الحركة"}</Button></>}
    >
      {!employee.active && <div className="mb-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm font-bold text-amber-900">الموظف غير نشط، ولن يقبل الخادم حركة مالية جديدة له.</div>}
      <form id="employee-transaction-form" className="grid gap-5 sm:grid-cols-2" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <FormField label="نوع الحركة" htmlFor="transaction-type" required error={errors.type?.message}>
          <Select id="transaction-type" {...register("type")}>{allowedTypes.map((type) => <option key={type} value={type}>{employeeTransactionTypeLabels[type]}</option>)}</Select>
        </FormField>
        <FormField label="المبلغ" htmlFor="transaction-amount" required error={errors.amount?.message}>
          <MoneyInput id="transaction-amount" {...register("amount")} />
        </FormField>
        <FormField label="تاريخ العمل" htmlFor="transaction-date" required error={errors.businessDate?.message}>
          <Input id="transaction-date" type="date" {...register("businessDate")} />
        </FormField>
        <FormField label="طريقة الدفع" htmlFor="transaction-payment" error={errors.paymentMethod?.message} hint="اختيارية، وتُستخدم عند وجود تدفق نقدي فعلي.">
          <Select id="transaction-payment" {...register("paymentMethod")}><option value="">بدون طريقة دفع</option>{paymentMethodsQuery.data?.content.map((method) => <option key={method.id} value={method.code}>{method.displayNameArabic}</option>)}</Select>
        </FormField>
        <div className="sm:col-span-2"><FormField label="ملاحظات" htmlFor="transaction-notes" error={errors.notes?.message}><Textarea id="transaction-notes" maxLength={2000} {...register("notes")} /></FormField></div>
        <div className="sm:col-span-2"><FormServerError message={errors.root?.server?.message || (paymentMethodsQuery.isError ? getArabicApiErrorMessage(paymentMethodsQuery.error) : undefined)} /></div>
      </form>
    </DialogShell>
  );
}
