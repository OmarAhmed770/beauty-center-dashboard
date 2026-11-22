import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { MoneyInput } from "@/components/shared/money-input";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { catalogKeys, createCatalogItem, searchCatalogCategories, updateCatalogItem } from "@/features/catalog/api";
import { catalogItemSchema, catalogItemTypeLabels, type CatalogItemFormValues } from "@/features/catalog/model";
import type { CatalogItem } from "@/features/catalog/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

const emptyValues: CatalogItemFormValues = { code: "", nameArabic: "", nameEnglish: "", itemType: "SERVICE", categoryId: "", currentSellingPrice: "0", currentCost: "0", active: true, description: "" };

export function CatalogItemFormDialog({ open, item, onOpenChange }: { open: boolean; item?: CatalogItem | null; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient();
  const categoriesQuery = useQuery({ queryKey: catalogKeys.categories({ active: true, size: 100 }), queryFn: () => searchCatalogCategories({ active: true, page: 0, size: 100 }), enabled: open, staleTime: 5 * 60_000 });
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<CatalogItemFormValues>({ resolver: zodResolver(catalogItemSchema), defaultValues: emptyValues });
  useEffect(() => {
    if (!open) return;
    reset(item ? { code: item.code, nameArabic: item.nameArabic, nameEnglish: item.nameEnglish, itemType: item.itemType, categoryId: String(item.categoryId), currentSellingPrice: String(item.currentSellingPrice), currentCost: String(item.currentCost), active: item.active, description: item.description ?? "" } : emptyValues);
  }, [item, open, reset]);
  const mutation = useMutation({
    mutationFn: (values: CatalogItemFormValues) => item
      ? updateCatalogItem(item.id, { nameArabic: values.nameArabic, nameEnglish: values.nameEnglish, categoryId: Number(values.categoryId), description: values.description || undefined })
      : createCatalogItem({ ...values, categoryId: Number(values.categoryId), description: values.description || undefined }),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: catalogKeys.all }); onOpenChange(false); },
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });
  const lockedClass = item ? "bg-stone-100" : undefined;

  return (
    <DialogShell open={open} title={item ? "تعديل بيانات الصنف" : "إضافة خدمة أو منتج"} description={item ? "الرمز والنوع والأسعار ثابتة هنا؛ استخدم إجراء تحديث السعر للتغييرات المالية." : "حدّد سعر البيع وسعر الشراء عند إضافة الصنف."} onOpenChange={onOpenChange} size="lg" footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button form="catalog-item-form" type="submit" disabled={mutation.isPending || categoriesQuery.isPending}>{mutation.isPending ? "جارٍ الحفظ..." : "حفظ الصنف"}</Button></>}>
      <form id="catalog-item-form" className="grid gap-5 sm:grid-cols-2" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <FormField label="الرمز" htmlFor="catalog-code" required error={errors.code?.message}><Input id="catalog-code" dir="ltr" readOnly={Boolean(item)} className={lockedClass} {...register("code")} /></FormField>
        <FormField label="النوع" htmlFor="catalog-type" required error={errors.itemType?.message}>
          {item ? <><Input id="catalog-type" value={catalogItemTypeLabels[item.itemType]} readOnly className={lockedClass} /><input type="hidden" {...register("itemType")} /></> : <Select id="catalog-type" {...register("itemType")}>{Object.entries(catalogItemTypeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</Select>}
        </FormField>
        <FormField label="الاسم العربي" htmlFor="catalog-name-ar" required error={errors.nameArabic?.message}><Input id="catalog-name-ar" {...register("nameArabic")} /></FormField>
        <FormField label="الاسم الإنجليزي" htmlFor="catalog-name-en" required error={errors.nameEnglish?.message}><Input id="catalog-name-en" dir="ltr" {...register("nameEnglish")} /></FormField>
        <FormField label="التصنيف" htmlFor="catalog-category" required error={errors.categoryId?.message}><Select id="catalog-category" {...register("categoryId")}><option value="">اختر التصنيف</option>{categoriesQuery.data?.content.map((category) => <option key={category.id} value={category.id}>{category.nameArabic}</option>)}</Select></FormField>
        <span />
        {item && !item.active && Number(item.currentSellingPrice) === 0 && <div className="sm:col-span-2 flex items-center gap-2 rounded-xl bg-amber-50 p-3 text-sm font-bold text-amber-900"><StatusBadge tone="warning">حدّد السعر</StatusBadge>هذا الصنف التمهيدي غير نشط ولن يظهر في البيع حتى تحدد سعره وتفعّله.</div>}
        <FormField label="سعر البيع" htmlFor="catalog-price" required error={errors.currentSellingPrice?.message}><MoneyInput id="catalog-price" readOnly={Boolean(item)} className={lockedClass} {...register("currentSellingPrice")} /></FormField>
        <FormField label="سعر الشراء" htmlFor="catalog-cost" required error={errors.currentCost?.message}><MoneyInput id="catalog-cost" readOnly={Boolean(item)} className={lockedClass} {...register("currentCost")} /></FormField>
        <div className="sm:col-span-2"><FormField label="الوصف" htmlFor="catalog-description" error={errors.description?.message}><Textarea id="catalog-description" maxLength={2000} {...register("description")} /></FormField></div>
        <div className="sm:col-span-2"><FormServerError message={errors.root?.server?.message || (categoriesQuery.isError ? getArabicApiErrorMessage(categoriesQuery.error) : undefined)} /></div>
      </form>
    </DialogShell>
  );
}
