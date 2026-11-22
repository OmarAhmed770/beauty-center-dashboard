import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { MoneyInput } from "@/components/shared/money-input";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { catalogKeys, updateCatalogPrices } from "@/features/catalog/api";
import { priceUpdateSchema, type PriceUpdateFormValues } from "@/features/catalog/model";
import type { CatalogItem } from "@/features/catalog/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { formatEgp } from "@/lib/format";

export function PriceUpdateDialog({ open, item, onOpenChange }: { open: boolean; item: CatalogItem | null; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient();
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<PriceUpdateFormValues>({ resolver: zodResolver(priceUpdateSchema), defaultValues: { sellingPrice: "", cost: "", reason: "" } });
  useEffect(() => { if (open) reset({ sellingPrice: "", cost: "", reason: "" }); }, [open, reset]);
  const mutation = useMutation({ mutationFn: (values: PriceUpdateFormValues) => updateCatalogPrices(item!.id, { sellingPrice: values.sellingPrice || undefined, cost: values.cost || undefined, reason: values.reason }), onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: catalogKeys.all }); onOpenChange(false); }, onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }) });
  return <DialogShell open={open} title={`تحديث سعر البيع وسعر الشراء — ${item?.nameArabic ?? ""}`} description="كل قيمة تتغير ستنشئ سجل سعر مستقلًا، والسبب مطلوب للتدقيق." onOpenChange={onOpenChange} footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button form="price-update-form" type="submit" disabled={mutation.isPending || !item}>{mutation.isPending ? "جارٍ التحديث..." : "تحديث وحفظ السجل"}</Button></>}><form id="price-update-form" className="grid gap-5" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate><div className="grid gap-3 rounded-xl bg-stone-50 p-4 sm:grid-cols-2"><p className="text-sm"><span className="text-stone-500">سعر البيع الحالي</span><strong className="mt-1 block text-stone-950">{formatEgp(item?.currentSellingPrice ?? 0)}</strong></p><p className="text-sm"><span className="text-stone-500">سعر الشراء الحالي</span><strong className="mt-1 block text-stone-950">{formatEgp(item?.currentCost ?? 0)}</strong></p></div><FormField label="سعر البيع الجديد" htmlFor="new-selling-price" error={errors.sellingPrice?.message} hint="اتركه فارغًا إذا لم يتغير."><MoneyInput id="new-selling-price" {...register("sellingPrice")} /></FormField><FormField label="سعر الشراء الجديد" htmlFor="new-cost" error={errors.cost?.message} hint="اتركه فارغًا إذا لم يتغير."><MoneyInput id="new-cost" {...register("cost")} /></FormField><FormField label="سبب التغيير" htmlFor="price-reason" required error={errors.reason?.message}><Textarea id="price-reason" maxLength={500} {...register("reason")} /></FormField><FormServerError message={errors.root?.server?.message} /></form></DialogShell>;
}
