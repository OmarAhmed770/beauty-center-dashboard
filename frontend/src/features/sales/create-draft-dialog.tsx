import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { cairoDateIso } from "@/components/shared/date-range-filter";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { createDraftInvoice, invoiceKeys } from "@/features/sales/api";
import { draftInvoiceSchema, type DraftInvoiceFormValues } from "@/features/sales/model";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

export function CreateDraftDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<DraftInvoiceFormValues>({ resolver: zodResolver(draftInvoiceSchema), defaultValues: { businessDate: cairoDateIso(), customerName: "", customerPhone: "", notes: "" } });
  useEffect(() => { if (open) reset({ businessDate: cairoDateIso(), customerName: "", customerPhone: "", notes: "" }); }, [open, reset]);
  const mutation = useMutation({ mutationFn: (values: DraftInvoiceFormValues) => createDraftInvoice({ businessDate: values.businessDate, customerName: values.customerName || undefined, customerPhone: values.customerPhone || undefined, notes: values.notes || undefined }), onSuccess: async (invoice) => { await queryClient.invalidateQueries({ queryKey: invoiceKeys.all }); onOpenChange(false); navigate(`/sales/invoices/${invoice.id}/edit`); }, onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }) });
  return <DialogShell open={open} title="فاتورة جديدة" description="احفظ بيانات الفاتورة كمسودة، ثم أضف البنود والدفعات قبل الإكمال." onOpenChange={onOpenChange} size="lg" footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button form="create-draft-form" type="submit" disabled={mutation.isPending}>{mutation.isPending ? "جارٍ إنشاء المسودة..." : "حفظ المسودة وفتحها"}</Button></>}><form id="create-draft-form" className="grid gap-5 sm:grid-cols-2" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate><FormField label="تاريخ العمل" htmlFor="invoice-business-date" required error={errors.businessDate?.message}><Input id="invoice-business-date" type="date" {...register("businessDate")} /></FormField><span /><FormField label="اسم العميل" htmlFor="customer-name" error={errors.customerName?.message}><Input id="customer-name" maxLength={150} placeholder="اختياري" {...register("customerName")} /></FormField><FormField label="هاتف العميل" htmlFor="customer-phone" error={errors.customerPhone?.message}><Input id="customer-phone" maxLength={50} placeholder="اختياري" dir="ltr" {...register("customerPhone")} /></FormField><div className="sm:col-span-2"><FormField label="ملاحظات" htmlFor="invoice-notes" error={errors.notes?.message}><Textarea id="invoice-notes" maxLength={2000} {...register("notes")} /></FormField></div><div className="sm:col-span-2"><FormServerError message={errors.root?.server?.message} /></div></form></DialogShell>;
}
