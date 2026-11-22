import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { createExpenseCategory, expenseCategoryKeys, updateExpenseCategory } from "@/features/expense-categories/api";
import type { ExpenseCategory } from "@/features/expense-categories/types";
import { expenseCategorySchema, type ExpenseCategoryFormValues } from "@/features/expenses/model";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

export function CategoryFormDialog({ open, category, onOpenChange }: { open: boolean; category?: ExpenseCategory | null; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient();
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<ExpenseCategoryFormValues>({
    resolver: zodResolver(expenseCategorySchema),
    defaultValues: { nameArabic: "", nameEnglish: "", active: true, notes: "" },
  });

  useEffect(() => {
    if (!open) return;
    reset(category ? { nameArabic: category.nameArabic, nameEnglish: category.nameEnglish, active: category.active, notes: category.notes ?? "" } : { nameArabic: "", nameEnglish: "", active: true, notes: "" });
  }, [category, open, reset]);

  const mutation = useMutation({
    mutationFn: (values: ExpenseCategoryFormValues) => category ? updateExpenseCategory(category.id, values) : createExpenseCategory(values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: expenseCategoryKeys.all });
      onOpenChange(false);
    },
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });

  return (
    <DialogShell open={open} title={category ? "تعديل تصنيف المصروف" : "إضافة تصنيف مصروف"} description="تظهر التصنيفات النشطة فقط عند تسجيل مصروف جديد." onOpenChange={onOpenChange} footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button type="submit" form="category-form" disabled={mutation.isPending}>{mutation.isPending ? "جارٍ الحفظ..." : "حفظ التصنيف"}</Button></>}>
      <form id="category-form" className="grid gap-5" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <FormField label="الاسم العربي" htmlFor="category-name-ar" required error={errors.nameArabic?.message}><Input id="category-name-ar" maxLength={150} {...register("nameArabic")} /></FormField>
        <FormField label="الاسم الإنجليزي" htmlFor="category-name-en" required error={errors.nameEnglish?.message}><Input id="category-name-en" maxLength={150} dir="ltr" {...register("nameEnglish")} /></FormField>
        <FormField label="ملاحظات" htmlFor="category-notes" error={errors.notes?.message}><Textarea id="category-notes" maxLength={2000} {...register("notes")} /></FormField>
        <FormServerError message={errors.root?.server?.message} />
      </form>
    </DialogShell>
  );
}
