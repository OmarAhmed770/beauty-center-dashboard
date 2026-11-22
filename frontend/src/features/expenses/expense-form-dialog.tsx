import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { cairoDateIso } from "@/components/shared/date-range-filter";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { MoneyInput } from "@/components/shared/money-input";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { expenseCategoryKeys, searchExpenseCategories } from "@/features/expense-categories/api";
import { createExpense, expenseKeys } from "@/features/expenses/api";
import { createExpenseSchema, type ExpenseFormValues } from "@/features/expenses/model";
import type { Expense } from "@/features/expenses/types";
import { paymentMethodKeys, searchPaymentMethods } from "@/features/payment-methods/api";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { invalidateFinancialViews } from "@/lib/financial-query-invalidation";

const emptyValues: ExpenseFormValues = {
  categoryId: "",
  amount: "",
  businessDate: cairoDateIso(),
  paymentMethodId: "",
  description: "",
  notes: "",
};

export function ExpenseFormDialog({ open, onOpenChange, onSaved }: { open: boolean; onOpenChange: (open: boolean) => void; onSaved?: (expense: Expense) => void }) {
  const queryClient = useQueryClient();
  const categoriesQuery = useQuery({
    queryKey: [...expenseCategoryKeys.all, "active"],
    queryFn: () => searchExpenseCategories({ active: true, page: 0, size: 100 }),
    enabled: open,
    staleTime: 5 * 60_000,
  });
  const paymentMethodsQuery = useQuery({
    queryKey: [...paymentMethodKeys.all, "active"],
    queryFn: () => searchPaymentMethods({ active: true, page: 0, size: 100 }),
    enabled: open,
    staleTime: 5 * 60_000,
  });
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<ExpenseFormValues>({
    resolver: zodResolver(createExpenseSchema),
    defaultValues: emptyValues,
  });

  useEffect(() => {
    if (!open) return;
    reset({ ...emptyValues, businessDate: cairoDateIso() });
  }, [open, reset]);

  const mutation = useMutation({
    mutationFn: (values: ExpenseFormValues) => createExpense({
      categoryId: Number(values.categoryId),
      amount: values.amount,
      businessDate: values.businessDate,
      paymentMethodId: Number(values.paymentMethodId),
      description: values.description,
      notes: values.notes || undefined,
    }),
    onSuccess: async (saved) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: expenseKeys.all }),
        invalidateFinancialViews(queryClient),
      ]);
      onSaved?.(saved);
      onOpenChange(false);
    },
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });

  const dependencyError = categoriesQuery.isError
    ? getArabicApiErrorMessage(categoriesQuery.error)
    : paymentMethodsQuery.isError
      ? getArabicApiErrorMessage(paymentMethodsQuery.error)
      : undefined;

  return (
    <DialogShell
      open={open}
      title="تسجيل مصروف جديد"
      description="سجّل المصروف في خطوة واحدة. لا يمكن حذف السجل بعد الحفظ."
      onOpenChange={onOpenChange}
      size="lg"
      footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button type="submit" form="expense-form" disabled={mutation.isPending || categoriesQuery.isPending || paymentMethodsQuery.isPending}>{mutation.isPending ? "جارٍ الحفظ..." : "حفظ المصروف"}</Button></>}
    >
      <form id="expense-form" className="grid gap-5 sm:grid-cols-2" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <FormField label="التصنيف" htmlFor="expense-category" required error={errors.categoryId?.message}>
          <Select id="expense-category" {...register("categoryId")}><option value="">اختر تصنيف المصروف</option>{categoriesQuery.data?.content.map((category) => <option key={category.id} value={category.id}>{category.nameArabic}</option>)}</Select>
        </FormField>
        <FormField label="المبلغ" htmlFor="expense-amount" required error={errors.amount?.message}>
          <MoneyInput id="expense-amount" {...register("amount")} />
        </FormField>
        <FormField label="تاريخ المصروف" htmlFor="expense-date" required error={errors.businessDate?.message}>
          <Input id="expense-date" type="date" {...register("businessDate")} />
        </FormField>
        <FormField label="طريقة الدفع" htmlFor="expense-payment" required error={errors.paymentMethodId?.message}>
          <Select id="expense-payment" {...register("paymentMethodId")}><option value="">اختر طريقة الدفع</option>{paymentMethodsQuery.data?.content.map((method) => <option key={method.id} value={method.id}>{method.displayNameArabic}</option>)}</Select>
        </FormField>
        <div className="sm:col-span-2"><FormField label="وصف المصروف" htmlFor="expense-description" required error={errors.description?.message}><Textarea id="expense-description" maxLength={1000} {...register("description")} /></FormField></div>
        <div className="sm:col-span-2"><FormField label="ملاحظات" htmlFor="expense-notes" error={errors.notes?.message}><Textarea id="expense-notes" maxLength={2000} {...register("notes")} /></FormField></div>
        <div className="sm:col-span-2"><FormServerError message={errors.root?.server?.message || dependencyError} /></div>
      </form>
    </DialogShell>
  );
}
