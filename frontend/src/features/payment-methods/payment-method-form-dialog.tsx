import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { createPaymentMethod, paymentMethodKeys, updatePaymentMethod } from "@/features/payment-methods/api";
import type { PaymentMethod } from "@/features/payment-methods/types";
import { paymentMethodSchema, paymentMethodTypeLabels, type PaymentMethodFormValues } from "@/features/expenses/model";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

export function PaymentMethodFormDialog({ open, paymentMethod, onOpenChange }: { open: boolean; paymentMethod?: PaymentMethod | null; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient();
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<PaymentMethodFormValues>({
    resolver: zodResolver(paymentMethodSchema),
    defaultValues: { code: "", displayNameArabic: "", displayNameEnglish: "", type: "OTHER", active: true, requiresReferenceNumber: false, sortOrder: 0 },
  });

  useEffect(() => {
    if (!open) return;
    reset(paymentMethod ? { code: paymentMethod.code, displayNameArabic: paymentMethod.displayNameArabic, displayNameEnglish: paymentMethod.displayNameEnglish, type: paymentMethod.type, active: paymentMethod.active, requiresReferenceNumber: paymentMethod.requiresReferenceNumber, sortOrder: paymentMethod.sortOrder } : { code: "", displayNameArabic: "", displayNameEnglish: "", type: "OTHER", active: true, requiresReferenceNumber: false, sortOrder: 0 });
  }, [open, paymentMethod, reset]);

  const mutation = useMutation({
    mutationFn: (values: PaymentMethodFormValues) => paymentMethod ? updatePaymentMethod(paymentMethod.id, values) : createPaymentMethod(values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: paymentMethodKeys.all });
      onOpenChange(false);
    },
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });

  return (
    <DialogShell open={open} title={paymentMethod ? "تعديل طريقة الدفع" : "إضافة طريقة دفع"} description={paymentMethod ? "الرمز ثابت بعد الإنشاء حفاظًا على السجل المالي." : "استخدم رمزًا إنجليزيًا ثابتًا ومميزًا."} onOpenChange={onOpenChange} size="lg" footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button type="submit" form="payment-method-form" disabled={mutation.isPending}>{mutation.isPending ? "جارٍ الحفظ..." : "حفظ طريقة الدفع"}</Button></>}>
      <form id="payment-method-form" className="grid gap-5 sm:grid-cols-2" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <FormField label="الرمز" htmlFor="payment-code" required error={errors.code?.message} hint={paymentMethod ? "لا يمكن تعديل الرمز بعد الإنشاء." : undefined}><Input id="payment-code" maxLength={50} dir="ltr" readOnly={Boolean(paymentMethod)} className={paymentMethod ? "bg-stone-100" : undefined} {...register("code")} /></FormField>
        <FormField label="النوع" htmlFor="payment-type" required error={errors.type?.message}><Select id="payment-type" {...register("type")}>{Object.entries(paymentMethodTypeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</Select></FormField>
        <FormField label="الاسم العربي" htmlFor="payment-name-ar" required error={errors.displayNameArabic?.message}><Input id="payment-name-ar" maxLength={150} {...register("displayNameArabic")} /></FormField>
        <FormField label="الاسم الإنجليزي" htmlFor="payment-name-en" required error={errors.displayNameEnglish?.message}><Input id="payment-name-en" maxLength={150} dir="ltr" {...register("displayNameEnglish")} /></FormField>
        <FormField label="ترتيب الظهور" htmlFor="payment-sort" required error={errors.sortOrder?.message}><Input id="payment-sort" type="number" min={0} max={10000} {...register("sortOrder", { valueAsNumber: true })} /></FormField>
        <div className="flex items-center pt-7"><label className="flex min-h-11 items-center gap-3 rounded-xl border border-stone-200 px-4 text-sm font-bold text-stone-700"><input type="checkbox" className="h-5 w-5 accent-rose-700" {...register("requiresReferenceNumber")} />يتطلب رقمًا مرجعيًا</label></div>
        <div className="sm:col-span-2"><FormServerError message={errors.root?.server?.message} /></div>
      </form>
    </DialogShell>
  );
}
