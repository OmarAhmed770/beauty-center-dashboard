import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { MoneyInput } from "@/components/shared/money-input";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/auth/auth-context";
import { catalogKeys, searchCatalogItems } from "@/features/catalog/api";
import { catalogItemTypeLabels } from "@/features/catalog/model";
import { searchDiscounts } from "@/features/discounts/api";
import { employeeKeys, searchEmployees } from "@/features/employees/api";
import { hairKeys, searchHairConfigurations } from "@/features/hair/api";
import { addInvoiceItem, invoiceKeys } from "@/features/sales/api";
import { invoiceItemSchema, type InvoiceItemFormValues } from "@/features/sales/model";
import type { InvoiceDetails } from "@/features/sales/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { formatEgp } from "@/lib/format";

export function AddInvoiceItemDialog({ open, invoice, onOpenChange }: { open: boolean; invoice: InvoiceDetails; onOpenChange: (open: boolean) => void }) {
  const { hasPermission } = useAuth();
  const canOverride = hasPermission("INVOICES_AND_RETURNS");
  const queryClient = useQueryClient();
  const [catalogSearch, setCatalogSearch] = useState("");
  const [selectedItemId, setSelectedItemId] = useState("");
  const [selectedConfigurationId, setSelectedConfigurationId] = useState("");
  const itemsQuery = useQuery({ queryKey: catalogKeys.items({ active: true, query: catalogSearch, size: 100 }), queryFn: () => searchCatalogItems({ active: true, query: catalogSearch || undefined, page: 0, size: 100 }), enabled: open, staleTime: 30_000 });
  const selectedItem = itemsQuery.data?.content.find((item) => String(item.id) === selectedItemId);
  const configurationsQuery = useQuery({ queryKey: hairKeys.list({ hairItemId: selectedItem?.id, active: true, effectiveOn: invoice.businessDate, size: 100 }), queryFn: () => searchHairConfigurations({ hairItemId: selectedItem!.id, active: true, effectiveOn: invoice.businessDate, page: 0, size: 100 }), enabled: open && selectedItem?.itemType === "HAIR_PRODUCT", staleTime: 30_000 });
  const employeesQuery = useQuery({ queryKey: employeeKeys.list({ active: true, page: 0, size: 100 }), queryFn: () => searchEmployees({ active: true, page: 0, size: 100 }), enabled: open, staleTime: 60_000 });
  const discountsQuery = useQuery({ queryKey: ["discounts", "invoice-item", invoice.businessDate], queryFn: () => searchDiscounts({ active: true, applicableDate: invoice.businessDate, page: 0, size: 100 }), enabled: open, staleTime: 30_000 });
  const { register, handleSubmit, watch, reset, setValue, setError, formState: { errors } } = useForm<InvoiceItemFormValues>({ resolver: zodResolver(invoiceItemSchema), defaultValues: { catalogItemId: "", hairConfigurationId: "", quantity: "1", discountId: "", employeeId: "", priceOverrideAmount: "", priceOverrideReason: "" } });
  const quantity = watch("quantity");
  const discountId = watch("discountId");
  const overrideAmount = watch("priceOverrideAmount");
  const selectedConfiguration = configurationsQuery.data?.content.find((configuration) => String(configuration.id) === selectedConfigurationId);
  const applicableDiscounts = useMemo(() => discountsQuery.data?.content.filter((discount) => !discount.catalogItemId && !discount.categoryId || discount.catalogItemId === selectedItem?.id || discount.categoryId === selectedItem?.categoryId) ?? [], [discountsQuery.data?.content, selectedItem?.categoryId, selectedItem?.id]);
  const selectedDiscount = applicableDiscounts.find((discount) => String(discount.id) === discountId);
  const calculation = useMemo(() => {
    const unitPrice = Number(selectedConfiguration?.unitSellingPrice ?? selectedItem?.currentSellingPrice ?? 0);
    const fee = Number(selectedConfiguration?.installationFee ?? 0);
    const subtotal = Math.round((unitPrice * (Number(quantity) || 0) + fee) * 100) / 100;
    const discountAmount = selectedDiscount ? selectedDiscount.type === "PERCENTAGE" ? Math.round(subtotal * Number(selectedDiscount.value)) / 100 : Number(selectedDiscount.value) : 0;
    const calculatedFinal = Math.round((subtotal - discountAmount) * 100) / 100;
    return { unitPrice, fee, subtotal, discountAmount, calculatedFinal, final: overrideAmount ? Number(overrideAmount) : calculatedFinal };
  }, [overrideAmount, quantity, selectedConfiguration?.installationFee, selectedConfiguration?.unitSellingPrice, selectedDiscount, selectedItem?.currentSellingPrice]);
  const selectItem = (value: string) => { setSelectedItemId(value); setSelectedConfigurationId(""); setValue("catalogItemId", value, { shouldValidate: true }); setValue("hairConfigurationId", ""); setValue("discountId", ""); };
  const close = (next: boolean) => { if (!next) { setCatalogSearch(""); setSelectedItemId(""); setSelectedConfigurationId(""); reset(); } onOpenChange(next); };
  const mutation = useMutation({ mutationFn: (values: InvoiceItemFormValues) => addInvoiceItem(invoice.id, { catalogItemId: Number(values.catalogItemId), hairConfigurationId: values.hairConfigurationId ? Number(values.hairConfigurationId) : undefined, quantity: values.quantity, discountId: values.discountId ? Number(values.discountId) : undefined, employeeId: values.employeeId ? Number(values.employeeId) : undefined, priceOverrideAmount: canOverride && values.priceOverrideAmount ? values.priceOverrideAmount : undefined, priceOverrideReason: canOverride && values.priceOverrideReason ? values.priceOverrideReason : undefined }), onSuccess: async (updated) => { queryClient.setQueryData(invoiceKeys.detail(invoice.id), updated); await queryClient.invalidateQueries({ queryKey: invoiceKeys.all }); close(false); }, onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }) });
  const dependencyError = itemsQuery.isError ? getArabicApiErrorMessage(itemsQuery.error) : configurationsQuery.isError ? getArabicApiErrorMessage(configurationsQuery.error) : employeesQuery.isError ? getArabicApiErrorMessage(employeesQuery.error) : discountsQuery.isError ? getArabicApiErrorMessage(discountsQuery.error) : undefined;
  return <DialogShell open={open} title="إضافة خدمة أو منتج" description="يحسب الخادم البند ويحفظ لقطة السعر والتكلفة والخصم وقت الإضافة." onOpenChange={close} size="xl" footer={<><Button variant="outline" onClick={() => close(false)} disabled={mutation.isPending}>رجوع</Button><Button form="invoice-item-form" type="submit" disabled={mutation.isPending || !selectedItem}>{mutation.isPending ? "جارٍ إضافة البند..." : "إضافة إلى الفاتورة"}</Button></>}><form id="invoice-item-form" className="grid gap-5 lg:grid-cols-3" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate><div className="lg:col-span-3"><FormField label="بحث سريع في الكتالوج" htmlFor="invoice-catalog-search"><Input id="invoice-catalog-search" value={catalogSearch} onChange={(event) => setCatalogSearch(event.target.value)} placeholder="اكتب اسم الخدمة أو المنتج أو الرمز" /></FormField></div><div className="lg:col-span-2"><FormField label="الخدمة أو المنتج" htmlFor="invoice-catalog-item" required error={errors.catalogItemId?.message}><Select id="invoice-catalog-item" value={selectedItemId} onChange={(event) => selectItem(event.target.value)}><option value="">اختر من النتائج</option>{itemsQuery.data?.content.map((item) => <option key={item.id} value={item.id}>{item.nameArabic} — {catalogItemTypeLabels[item.itemType]} — {formatEgp(item.currentSellingPrice)}</option>)}</Select><input type="hidden" {...register("catalogItemId")} /></FormField></div><FormField label="الكمية" htmlFor="invoice-quantity" required error={errors.quantity?.message}><Input id="invoice-quantity" type="number" min="1" step="1" inputMode="numeric" {...register("quantity")} /></FormField>{selectedItem?.itemType === "HAIR_PRODUCT" && <div className="lg:col-span-2"><FormField label="إعداد تركيب الشعر" htmlFor="invoice-hair-config" error={errors.hairConfigurationId?.message} hint="اختر الإعداد لتطبيق سعر الوحدة ورسوم التركيب الخاصة به."><Select id="invoice-hair-config" value={selectedConfigurationId} onChange={(event) => { setSelectedConfigurationId(event.target.value); setValue("hairConfigurationId", event.target.value); }}><option value="">بدون إعداد تركيب</option>{configurationsQuery.data?.content.map((configuration) => <option key={configuration.id} value={configuration.id}>{configuration.brand} — {configuration.length}{configuration.color ? ` — ${configuration.color}` : ""}</option>)}</Select><input type="hidden" {...register("hairConfigurationId")} /></FormField></div>}<FormField label="الموظف المنفذ" htmlFor="invoice-employee" error={errors.employeeId?.message} hint="اختياري"><Select id="invoice-employee" {...register("employeeId")}><option value="">غير محدد</option>{employeesQuery.data?.content.map((employee) => <option key={employee.id} value={employee.id}>{employee.name} — {employee.position}</option>)}</Select></FormField><FormField label="الخصم" htmlFor="invoice-discount" error={errors.discountId?.message}><Select id="invoice-discount" {...register("discountId")}><option value="">بدون خصم</option>{applicableDiscounts.map((discount) => <option key={discount.id} value={discount.id}>{discount.name}</option>)}</Select></FormField>{canOverride && <><FormField label="سعر نهائي يدوي" htmlFor="invoice-override" error={errors.priceOverrideAmount?.message} hint="يتطلب سببًا ويُحفظ في سجل التدقيق."><MoneyInput id="invoice-override" {...register("priceOverrideAmount")} /></FormField><FormField label="سبب السعر اليدوي" htmlFor="invoice-override-reason" error={errors.priceOverrideReason?.message}><Textarea id="invoice-override-reason" maxLength={500} {...register("priceOverrideReason")} /></FormField></>}<div className="lg:col-span-3"><Card className="grid gap-3 bg-stone-50 p-4 sm:grid-cols-3 lg:grid-cols-6"><Preview label="السعر الأصلي للوحدة" value={calculation.unitPrice} /><Preview label="رسوم التركيب" value={calculation.fee} /><Preview label="الإجمالي الأصلي" value={calculation.subtotal} /><Preview label="الخصم المتوقع" value={calculation.discountAmount} /><Preview label="الناتج المحسوب" value={calculation.calculatedFinal} invalid={calculation.calculatedFinal < 0} /><Preview label="النهائي المتوقع" value={calculation.final} accent /></Card>{calculation.calculatedFinal < 0 && <p className="mt-2 text-sm font-bold text-red-700">المبلغ الثابت أكبر من قيمة البند، وسيمنع الخادم تطبيقه.</p>}</div><div className="lg:col-span-3"><FormServerError message={errors.root?.server?.message || dependencyError} /></div></form></DialogShell>;
}

function Preview({ label, value, accent = false, invalid = false }: { label: string; value: number; accent?: boolean; invalid?: boolean }) { return <div><p className="text-xs font-bold text-stone-500">{label}</p><p className={`mt-2 font-black tabular-nums ${accent ? "text-rose-800" : invalid ? "text-red-700" : "text-stone-950"}`}>{formatEgp(value)}</p></div>; }
