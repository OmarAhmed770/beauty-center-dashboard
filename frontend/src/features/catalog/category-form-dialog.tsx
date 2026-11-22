import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { catalogKeys, createCatalogCategory, updateCatalogCategory } from "@/features/catalog/api";
import { catalogCategorySchema, type CatalogCategoryFormValues } from "@/features/catalog/model";
import type { CatalogCategory } from "@/features/catalog/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

export function CatalogCategoryFormDialog({ open, category, onOpenChange }: { open: boolean; category?: CatalogCategory | null; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient();
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<CatalogCategoryFormValues>({ resolver: zodResolver(catalogCategorySchema), defaultValues: { nameArabic: "", nameEnglish: "", active: true, description: "" } });
  useEffect(() => { if (open) reset(category ? { nameArabic: category.nameArabic, nameEnglish: category.nameEnglish, active: category.active, description: category.description ?? "" } : { nameArabic: "", nameEnglish: "", active: true, description: "" }); }, [category, open, reset]);
  const mutation = useMutation({
    mutationFn: (values: CatalogCategoryFormValues) => category ? updateCatalogCategory(category.id, values) : createCatalogCategory(values),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: catalogKeys.all }); onOpenChange(false); },
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });
  return <DialogShell open={open} title={category ? "تعديل تصنيف الكتالوج" : "إضافة تصنيف للكتالوج"} description="تستخدم التصنيفات النشطة عند إنشاء الخدمات والمنتجات." onOpenChange={onOpenChange} footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button form="catalog-category-form" type="submit" disabled={mutation.isPending}>{mutation.isPending ? "جارٍ الحفظ..." : "حفظ التصنيف"}</Button></>}><form id="catalog-category-form" className="grid gap-5" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate><FormField label="الاسم العربي" htmlFor="catalog-category-ar" required error={errors.nameArabic?.message}><Input id="catalog-category-ar" {...register("nameArabic")} /></FormField><FormField label="الاسم الإنجليزي" htmlFor="catalog-category-en" required error={errors.nameEnglish?.message}><Input id="catalog-category-en" dir="ltr" {...register("nameEnglish")} /></FormField><FormField label="الوصف" htmlFor="catalog-category-description" error={errors.description?.message}><Textarea id="catalog-category-description" maxLength={1000} {...register("description")} /></FormField><FormServerError message={errors.root?.server?.message} /></form></DialogShell>;
}
