import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { MoneyInput } from "@/components/shared/money-input";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/auth/auth-context";
import { catalogKeys, getCatalogItem } from "@/features/catalog/api";
import { searchDiscounts } from "@/features/discounts/api";
import { hairKeys, previewHairCalculation, searchHairConfigurations } from "@/features/hair/api";
import { hairPreviewSchema, type HairPreviewFormValues } from "@/features/hair/model";
import { cairoDateIso } from "@/components/shared/date-range-filter";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { formatMoney } from "@/lib/format";

export function HairPreviewPanel({ onAddConfiguration }: { onAddConfiguration?: () => void }) {
  const { hasPermission } = useAuth();
  const canViewCostAndOverride = hasPermission("SERVICES_AND_PRICES");
  const queryClient = useQueryClient();
  const [manualPriceOpen, setManualPriceOpen] = useState(false);
  const configurationsQuery = useQuery({ queryKey: hairKeys.list({ active: true, effectiveOn: cairoDateIso(), size: 100 }), queryFn: () => searchHairConfigurations({ active: true, effectiveOn: cairoDateIso(), page: 0, size: 100 }), staleTime: 60_000 });
  const discountsQuery = useQuery({ queryKey: ["discounts", "hair-preview", cairoDateIso()], queryFn: () => searchDiscounts({ active: true, applicableDate: cairoDateIso(), page: 0, size: 100 }), staleTime: 60_000 });
  const { register, handleSubmit, watch, resetField, setError, formState: { errors } } = useForm<HairPreviewFormValues>({ resolver: zodResolver(hairPreviewSchema), defaultValues: { configurationId: "", quantity: "1", discountId: "", overrideAmount: "", overrideReason: "" } });
  const values = watch();
  const selectedConfiguration = configurationsQuery.data?.content.find((configuration) => String(configuration.id) === values.configurationId);
  const selectedItemQuery = useQuery({ queryKey: catalogKeys.item(selectedConfiguration?.hairItemId ?? 0), queryFn: () => getCatalogItem(selectedConfiguration!.hairItemId), enabled: Boolean(selectedConfiguration), staleTime: 5 * 60_000 });
  const applicableDiscounts = useMemo(() => discountsQuery.data?.content.filter((discount) => !discount.catalogItemId && !discount.categoryId || discount.catalogItemId === selectedConfiguration?.hairItemId || discount.categoryId === selectedItemQuery.data?.categoryId) ?? [], [discountsQuery.data?.content, selectedConfiguration?.hairItemId, selectedItemQuery.data?.categoryId]);
  useEffect(() => { resetField("discountId"); }, [values.configurationId, resetField]);
  const previewMutation = useMutation({ mutationFn: (input: HairPreviewFormValues) => previewHairCalculation({ configurationId: Number(input.configurationId), quantity: input.quantity, discountId: input.discountId ? Number(input.discountId) : undefined, overrideAmount: canViewCostAndOverride && input.overrideAmount ? input.overrideAmount : undefined, overrideReason: canViewCostAndOverride && input.overrideReason ? input.overrideReason : undefined }), onSuccess: async (result) => { if (result.overridden) await queryClient.invalidateQueries({ queryKey: hairKeys.all }); }, onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }) });
  const validPreview = hairPreviewSchema.safeParse(values).success && !configurationsQuery.isPending && !configurationsQuery.isError;
  const closeManualPrice = () => { setManualPriceOpen(false); resetField("overrideAmount"); resetField("overrideReason"); };

  if (configurationsQuery.isPending) return <Card className="p-5 sm:p-6"><p className="text-sm font-bold text-muted">جارٍ تحميل إعدادات الشعر...</p></Card>;
  if (configurationsQuery.isError) return <Card className="p-5 sm:p-6"><FormServerError message={getArabicApiErrorMessage(configurationsQuery.error)} /></Card>;
  if (configurationsQuery.data.content.length === 0) return <Card className="p-5 text-center sm:p-6"><h2 className="text-lg font-black text-ink">مفيش إعدادات شعر لسه</h2><p className="mt-2 text-sm text-muted">ضيف أول إعداد عشان تحسب السعر بسهولة.</p>{onAddConfiguration && <Button className="mt-5" type="button" onClick={onAddConfiguration}>ضيف أول إعداد</Button>}</Card>;

  return <Card className="p-5 sm:p-6"><h2 className="text-lg font-black text-ink">معاينة تركيب الشعر</h2><form className="mt-6 grid gap-5 lg:grid-cols-3" onSubmit={handleSubmit((input) => previewMutation.mutate(input))} noValidate><div className="lg:col-span-2"><FormField label="نوع الشعر" htmlFor="preview-configuration" required error={errors.configurationId?.message}><Select id="preview-configuration" {...register("configurationId")}><option value="">اختر النوع</option>{configurationsQuery.data.content.map((configuration) => <option key={configuration.id} value={configuration.id}>{configuration.hairItemNameArabic} — {configuration.brand} — {configuration.length}{configuration.color ? ` — ${configuration.color}` : ""}</option>)}</Select></FormField></div><FormField label="الكمية بالجرام" htmlFor="preview-quantity" required error={errors.quantity?.message}><Input id="preview-quantity" type="number" min="1" step="1" inputMode="numeric" {...register("quantity")} /></FormField><FormField label="الخصم" htmlFor="preview-discount" error={errors.discountId?.message}><Select id="preview-discount" {...register("discountId")}><option value="">بدون خصم</option>{applicableDiscounts.map((discount) => <option key={discount.id} value={discount.id}>{discount.name}</option>)}</Select></FormField>{canViewCostAndOverride && !manualPriceOpen && <div className="flex items-end"><button type="button" className="min-h-11 text-sm font-black text-primary hover:underline" onClick={() => setManualPriceOpen(true)}>تعديل السعر يدويًا</button></div>}{canViewCostAndOverride && manualPriceOpen && <><FormField label="السعر اليدوي" htmlFor="preview-override" error={errors.overrideAmount?.message}><MoneyInput id="preview-override" {...register("overrideAmount")} /></FormField><FormField label="سبب تعديل السعر" htmlFor="preview-reason" error={errors.overrideReason?.message}><Textarea id="preview-reason" maxLength={500} {...register("overrideReason")} /></FormField><div className="flex items-end"><button type="button" className="min-h-11 text-sm font-black text-muted hover:underline" onClick={closeManualPrice}>إلغاء التعديل اليدوي</button></div></>}<div className="lg:col-span-3"><FormServerError message={errors.root?.server?.message || (discountsQuery.isError ? getArabicApiErrorMessage(discountsQuery.error) : undefined)} /></div><div className="lg:col-span-3"><Button type="submit" size="lg" disabled={previewMutation.isPending || !validPreview}>{previewMutation.isPending ? "جارٍ الحساب..." : "احسب المعاينة"}</Button></div></form>{previewMutation.data && <section className="mt-7 grid gap-3 border-t border-soft-border pt-6 sm:grid-cols-2 lg:grid-cols-4" aria-label="نتيجة حساب الشعر"><Result label="قيمة الشعر" value={previewMutation.data.baseProductAmount} /><Result label="رسوم التركيب" value={previewMutation.data.installationFee} /><Result label="الإجمالي قبل الخصم" value={previewMutation.data.subtotal} /><Result label="قيمة الخصم" value={previewMutation.data.discountAmount} /><Result label="السعر المحسوب" value={previewMutation.data.calculatedFinalAmount} /><Result label={previewMutation.data.overridden ? "السعر النهائي المعدّل" : "السعر النهائي"} value={previewMutation.data.finalAmount} accent />{canViewCostAndOverride && <Result label="تقدير سعر الشراء" value={previewMutation.data.costEstimate} />}</section>}</Card>;
}

function Result({ label, value, accent = false }: { label: string; value: number | string | null; accent?: boolean }) { return <div className={accent ? "rounded-xl bg-primary p-4 text-white" : "rounded-xl bg-surface-soft p-4"}><p className={accent ? "text-xs font-bold text-white/80" : "text-xs font-bold text-muted"}>{label}</p><bdi dir="ltr" className="mt-2 block text-lg font-black tabular-nums">{formatMoney(value)}</bdi></div>; }
