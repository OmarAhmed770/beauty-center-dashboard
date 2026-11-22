import { CheckCircle2, Minus, Plus, Save, Search, Trash2 } from "lucide-react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { cairoDateIso } from "@/components/shared/date-range-filter";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { useAuth } from "@/features/auth/auth-context";
import { catalogKeys, searchCatalogItems } from "@/features/catalog/api";
import type { CatalogItem } from "@/features/catalog/types";
import { employeeKeys, searchEmployees } from "@/features/employees/api";
import { hairKeys, previewHairCalculation, searchHairConfigurations } from "@/features/hair/api";
import type { HairConfiguration, HairCalculationPreview } from "@/features/hair/types";
import { paymentMethodKeys, searchPaymentMethods } from "@/features/payment-methods/api";
import { createQuickOrder, invoiceKeys } from "@/features/sales/api";
import type { QuickOrderInput } from "@/features/sales/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { invalidateFinancialViews } from "@/lib/financial-query-invalidation";
import { formatMoney, formatQuantity } from "@/lib/format";

const LAST_EMPLOYEE_KEY = "beauty-center.last-order-employee";

interface OrderLine {
  key: string;
  catalogItemId: number;
  quantity: string;
  employeeId: string;
  hairConfigurationId: string;
  hairGrams: string;
}

interface FormErrors {
  lines?: string;
  paymentMethod?: string;
  paymentReference?: string;
  customerPhone?: string;
}

function rememberedEmployeeId(): number | null {
  const value = Number(window.localStorage.getItem(LAST_EMPLOYEE_KEY));
  return Number.isInteger(value) && value > 0 ? value : null;
}

function requestId(): string {
  return globalThis.crypto?.randomUUID?.() ?? `order-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function positiveWholeNumber(value: string): number {
  const number = Number(value);
  return Number.isInteger(number) && number > 0 ? number : 0;
}

function minimumHairGrams(configuration: HairConfiguration | undefined): number {
  return Math.max(1, positiveWholeNumber(String(configuration?.minimumQuantity ?? "")) || 1);
}

function hasFractionalQuantity(value: string): boolean {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 && !Number.isInteger(number);
}

function isReasonablePhone(value: string): boolean {
  const trimmed = value.trim();
  return !trimmed || (/^[+0-9][0-9 ()-]{5,49}$/.test(trimmed) && (trimmed.match(/\d/g)?.length ?? 0) >= 6);
}

export function SimpleOrderPage() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const preferredEmployeeId = user?.employeeId ?? rememberedEmployeeId();
  const [query, setQuery] = useState("");
  const [lines, setLines] = useState<OrderLine[]>([]);
  const [paymentMethodId, setPaymentMethodId] = useState("");
  const [paymentReference, setPaymentReference] = useState("");
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [notes, setNotes] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string>();
  const [savedOrderNumber, setSavedOrderNumber] = useState<string>();
  const [clientRequestId, setClientRequestId] = useState(requestId);

  const catalogQuery = useQuery({
    queryKey: catalogKeys.items({ active: true, quickOrder: true }),
    queryFn: () => searchCatalogItems({ active: true, page: 0, size: 100 }),
    staleTime: 5 * 60_000,
  });
  const employeesQuery = useQuery({
    queryKey: [...employeeKeys.all, "quick-order"],
    queryFn: () => searchEmployees({ active: true, page: 0, size: 100 }),
    staleTime: 5 * 60_000,
  });
  const paymentMethodsQuery = useQuery({
    queryKey: [...paymentMethodKeys.all, "quick-order"],
    queryFn: () => searchPaymentMethods({ active: true, page: 0, size: 100 }),
    staleTime: 5 * 60_000,
  });
  const hasHairLine = lines.some((line) => catalogQuery.data?.content.some((item) => item.id === line.catalogItemId && item.itemType === "HAIR_PRODUCT"));
  const hairConfigurationsQuery = useQuery({
    queryKey: hairKeys.list({ active: true, quickOrder: true }),
    queryFn: () => searchHairConfigurations({ active: true, effectiveOn: cairoDateIso(), page: 0, size: 100 }),
    enabled: hasHairLine,
    staleTime: 5 * 60_000,
  });

  const catalogById = useMemo(
    () => new Map((catalogQuery.data?.content ?? []).map((item) => [item.id, item])),
    [catalogQuery.data],
  );
  const hairLines = useMemo(
    () => lines.filter((line) => catalogById.get(line.catalogItemId)?.itemType === "HAIR_PRODUCT"),
    [catalogById, lines],
  );
  const hairPreviews = useQueries({
    queries: hairLines.map((line) => ({
      queryKey: ["quick-order", "hair-preview", line.hairConfigurationId, line.hairGrams],
      queryFn: () => previewHairCalculation({ configurationId: Number(line.hairConfigurationId), quantity: line.hairGrams }),
      enabled: Number(line.hairConfigurationId) > 0 && positiveWholeNumber(line.hairGrams) > 0,
      staleTime: 20_000,
    })),
  });
  const hairPreviewByLineKey = useMemo(
    () => new Map(hairLines.map((line, index) => [line.key, hairPreviews[index]?.data])),
    [hairLines, hairPreviews],
  );
  const selectedPaymentMethod = paymentMethodsQuery.data?.content.find((method) => String(method.id) === paymentMethodId);
  const paymentReferenceRequired = selectedPaymentMethod?.requiresReferenceNumber === true;
  const cashMethod = useMemo(
    () => paymentMethodsQuery.data?.content.find((method) => method.code === "CASH"),
    [paymentMethodsQuery.data],
  );

  useEffect(() => {
    if (!paymentMethodId && cashMethod) setPaymentMethodId(String(cashMethod.id));
  }, [cashMethod, paymentMethodId]);

  useEffect(() => {
    if (!paymentReferenceRequired) {
      setPaymentReference("");
      setErrors((current) => ({ ...current, paymentReference: undefined }));
    }
  }, [paymentReferenceRequired]);

  useEffect(() => {
    const configurations = hairConfigurationsQuery.data?.content ?? [];
    if (configurations.length === 0) return;
    setLines((current) => {
      let changed = false;
      const next = current.map((line) => {
        if (catalogById.get(line.catalogItemId)?.itemType !== "HAIR_PRODUCT" || line.hairConfigurationId) {
          return line;
        }
        const configuration = configurations.find((entry) => entry.hairItemId === line.catalogItemId);
        if (!configuration) return line;
        changed = true;
        return {
          ...line,
          hairConfigurationId: String(configuration.id),
          hairGrams: String(minimumHairGrams(configuration)),
        };
      });
      return changed ? next : current;
    });
  }, [catalogById, hairConfigurationsQuery.data?.content]);

  const lineTotals = useMemo(() => new Map(lines.map((line) => {
    const item = catalogById.get(line.catalogItemId);
    if (!item) return [line.key, 0] as const;
    if (item.itemType === "HAIR_PRODUCT") {
      return [line.key, Number(hairPreviewByLineKey.get(line.key)?.finalAmount ?? 0)] as const;
    }
    return [line.key, Number(item.currentSellingPrice) * positiveWholeNumber(line.quantity)] as const;
  })), [catalogById, hairPreviewByLineKey, lines]);
  const total = Array.from(lineTotals.values()).reduce((sum, value) => sum + value, 0);
  const hasUnreadyHairLine = hairLines.some((line, index) => {
    const result = hairPreviews[index];
    return !line.hairConfigurationId || !positiveWholeNumber(line.hairGrams) || result?.isPending || result?.isError || !result?.data;
  });
  const hasInvalidHairQuantity = hairLines.some((line) => {
    const configuration = hairConfigurationsQuery.data?.content.find((entry) => String(entry.id) === line.hairConfigurationId);
    const grams = positiveWholeNumber(line.hairGrams);
    if (!configuration || !grams) return false;
    return (configuration.minimumQuantity !== null && grams < Number(configuration.minimumQuantity))
      || !configuration.stockConfigured
      || (configuration.availableStockGrams !== null && grams > Number(configuration.availableStockGrams));
  });

  const mutation = useMutation({
    mutationFn: (input: QuickOrderInput) => createQuickOrder(input),
    onSuccess: async (order, input) => {
      const employeeId = input.lines.find((line) => line.employeeId)?.employeeId;
      if (employeeId) window.localStorage.setItem(LAST_EMPLOYEE_KEY, String(employeeId));
      setSavedOrderNumber(order.invoiceNumber);
      setLines([]);
      setPaymentReference("");
      setCustomerName("");
      setCustomerPhone("");
      setNotes("");
      setErrors({});
      setServerError(undefined);
      setClientRequestId(requestId());
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: invoiceKeys.all }),
        invalidateFinancialViews(queryClient),
      ]);
    },
    onError: (error) => setServerError(getArabicApiErrorMessage(error)),
  });

  const changeLine = (key: string, change: Partial<OrderLine>) => {
    setLines((current) => current.map((line) => {
      if (line.key !== key) return line;
      const next = { ...line, ...change };
      if (catalogById.get(next.catalogItemId)?.itemType !== "HAIR_PRODUCT") return next;
      const selectedConfiguration = hairConfigurationsQuery.data?.content.find(
        (configuration) => String(configuration.id) === next.hairConfigurationId,
      );
      if (!selectedConfiguration) return next;
      const minimumGrams = minimumHairGrams(selectedConfiguration);
      return {
        ...next,
        hairGrams: change.hairConfigurationId !== undefined
          ? String(minimumGrams)
          : String(Math.max(minimumGrams, positiveWholeNumber(next.hairGrams) || minimumGrams)),
      };
    }));
    setServerError(undefined);
  };
  const addItem = (item: CatalogItem) => {
    if (item.itemType === "HAIR_PRODUCT") {
      const existingHairLine = lines.find((line) => catalogById.get(line.catalogItemId)?.itemType === "HAIR_PRODUCT");
      if (existingHairLine) {
        if (existingHairLine.catalogItemId === item.id) {
          changeLine(existingHairLine.key, { hairGrams: String(positiveWholeNumber(existingHairLine.hairGrams) + 1) });
          return;
        }
        setErrors((current) => ({ ...current, lines: "يمكن إضافة نوع شعر واحد فقط في الأوردر الواحد" }));
        return;
      }
    }
    const existing = lines.find((line) => line.catalogItemId === item.id && item.itemType !== "HAIR_PRODUCT");
    if (existing) {
      changeLine(existing.key, { quantity: String(positiveWholeNumber(existing.quantity) + 1) });
      return;
    }
    const configuration = hairConfigurationsQuery.data?.content.find((entry) => entry.hairItemId === item.id);
    setLines((current) => [...current, {
      key: `${item.id}-${Date.now()}-${current.length}`,
      catalogItemId: item.id,
      quantity: "1",
      employeeId: preferredEmployeeId ? String(preferredEmployeeId) : "",
      hairConfigurationId: configuration ? String(configuration.id) : "",
      hairGrams: String(minimumHairGrams(configuration)),
    }]);
    setErrors((current) => ({ ...current, lines: undefined }));
    setServerError(undefined);
  };
  const submit = () => {
    const nextErrors: FormErrors = {};
    if (lines.length === 0) nextErrors.lines = "اختر خدمة واحدة على الأقل";
    if (!paymentMethodId) nextErrors.paymentMethod = "اختر طريقة الدفع";
    if (paymentReferenceRequired && !paymentReference.trim()) nextErrors.paymentReference = "رقم العملية أو مرجع الدفع مطلوب";
    if (!isReasonablePhone(customerPhone)) nextErrors.customerPhone = "اكتب رقم موبايل صحيح أو اتركه فارغًا";
    if (hasUnreadyHairLine) nextErrors.lines = "أكمل بيانات الشعر وانتظر ظهور السعر";
    if (hasInvalidHairQuantity) nextErrors.lines = "تحقق من الحد الأدنى وكمية مخزون الشعر المتاحة";
    if (lines.some((line) => hasFractionalQuantity(line.quantity) || hasFractionalQuantity(line.hairGrams))) nextErrors.lines = "الكمية والجرامات لازم تكون أرقام صحيحة";
    if (lines.length > 0 && total <= 0) nextErrors.lines = "تحقق من الكمية والسعر قبل الحفظ";
    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }
    setSavedOrderNumber(undefined);
    setServerError(undefined);
    mutation.mutate({
      businessDate: cairoDateIso(),
      clientRequestId,
      ...(customerName.trim() ? { customerName: customerName.trim() } : {}),
      ...(customerPhone.trim() ? { customerPhone: customerPhone.trim() } : {}),
      ...(notes.trim() ? { notes: notes.trim() } : {}),
      lines: lines.map((line) => ({
        catalogItemId: line.catalogItemId,
        quantity: line.quantity,
        ...(line.employeeId ? { employeeId: Number(line.employeeId) } : {}),
        ...(line.hairConfigurationId ? { hairConfigurationId: Number(line.hairConfigurationId) } : {}),
        ...(catalogById.get(line.catalogItemId)?.itemType === "HAIR_PRODUCT" ? { hairGrams: line.hairGrams } : {}),
      })),
      payments: [{
        paymentMethodId: Number(paymentMethodId),
        ...(paymentReferenceRequired ? { referenceNumber: paymentReference.trim() } : {}),
      }],
    });
  };

  if (catalogQuery.isPending || employeesQuery.isPending || paymentMethodsQuery.isPending || (hasHairLine && hairConfigurationsQuery.isPending)) return <LoadingState label="جارٍ تجهيز الأوردر..." />;
  const dependencyError = [catalogQuery, employeesQuery, paymentMethodsQuery].find((result) => result.isError)?.error;
  if (dependencyError) return <FormServerError message={getArabicApiErrorMessage(dependencyError)} />;
  const normalizedQuery = query.trim().toLocaleLowerCase();
  const visibleItems = (catalogQuery.data?.content ?? []).filter((item) => !normalizedQuery
    || item.nameArabic.toLocaleLowerCase().includes(normalizedQuery)
    || item.nameEnglish.toLocaleLowerCase().includes(normalizedQuery));
  const itemsByCategory = visibleItems.reduce<Map<string, CatalogItem[]>>((groups, item) => {
    const group = groups.get(item.categoryNameArabic) ?? [];
    group.push(item);
    groups.set(item.categoryNameArabic, group);
    return groups;
  }, new Map());

  return <div className="mx-auto max-w-4xl space-y-5">
    <PageHeader title="أوردر جديد" description="اختار الخدمة، حدّد طريقة الدفع، واحفظ في خطوة واحدة." />
    {savedOrderNumber && <div className="flex items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-base font-bold text-emerald-900" role="status"><CheckCircle2 className="h-6 w-6 shrink-0" />تم حفظ الأوردر رقم <span dir="ltr" className="font-mono">{savedOrderNumber}</span></div>}
    <Card className="p-4 sm:p-6"><h2 className="text-lg font-black text-ink">بيانات العميلة <span className="text-sm font-bold text-muted">(اختياري)</span></h2><div className="mt-4 grid gap-4 sm:grid-cols-2"><FormField label="اسم العميلة" htmlFor="quick-order-customer-name"><Input id="quick-order-customer-name" value={customerName} onChange={(event) => setCustomerName(event.target.value)} maxLength={150} autoComplete="name" /></FormField><FormField label="رقم الموبايل" htmlFor="quick-order-customer-phone" error={errors.customerPhone}><Input id="quick-order-customer-phone" value={customerPhone} onChange={(event) => { setCustomerPhone(event.target.value); setErrors((current) => ({ ...current, customerPhone: undefined })); }} type="tel" inputMode="tel" maxLength={50} autoComplete="tel" dir="ltr" /></FormField></div><div className="mt-4"><FormField label="ملاحظات" htmlFor="quick-order-notes"><Input id="quick-order-notes" value={notes} onChange={(event) => setNotes(event.target.value)} maxLength={2000} /></FormField></div></Card>
    <Card className="p-4 sm:p-6"><h2 className="mb-4 text-lg font-black text-ink">اختيار الخدمات</h2><FormField label="ابحث عن خدمة" htmlFor="quick-order-search" required error={errors.lines}><div className="relative"><Search className="pointer-events-none absolute end-4 top-1/2 h-5 w-5 -translate-y-1/2 text-stone-400" /><Input id="quick-order-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="ابحث باسم الخدمة أو المنتج" className="pe-12" autoFocus /></div></FormField>{visibleItems.length === 0 ? <div className="mt-5 rounded-2xl border border-dashed border-[#e9dfe4] bg-[#fffafb] p-6 text-center text-sm font-bold text-[#736873]">مفيش خدمات لسه، اطلب من الأونر يضيف الخدمات والأسعار.</div> : <div className="mt-5 space-y-5">{Array.from(itemsByCategory).map(([category, items]) => <section key={category}><h3 className="mb-3 text-sm font-black text-[#736873]">{category}</h3><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{items.map((item) => <button key={item.id} type="button" onClick={() => addItem(item)} className="min-h-24 rounded-2xl border border-[#e9dfe4] bg-white p-4 text-start shadow-sm transition hover:border-[#E88DB4] hover:bg-[#fff8fb] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A83868]" aria-label={`إضافة ${item.nameArabic}`}><p className="font-black text-[#29252A]">{item.nameArabic}</p><p className="mt-2 text-sm font-bold tabular-nums text-[#A83868]">{item.itemType === "HAIR_PRODUCT" ? "يُحسب حسب الجرام" : formatMoney(item.currentSellingPrice)}</p></button>)}</div></section>)}</div>}</Card>
    {lines.length > 0 && <Card className="space-y-4 p-4 sm:p-6"><h2 className="text-lg font-black text-ink">الخدمات المختارة</h2>{lines.map((line) => <OrderLineEditor key={line.key} line={line} item={catalogById.get(line.catalogItemId)} employees={employeesQuery.data?.content ?? []} hairConfigurations={hairConfigurationsQuery.data?.content ?? []} hairConfigurationError={hairConfigurationsQuery.isError ? getArabicApiErrorMessage(hairConfigurationsQuery.error) : undefined} hairPreview={hairPreviewByLineKey.get(line.key)} total={lineTotals.get(line.key) ?? 0} onChange={changeLine} onRemove={() => setLines((current) => current.filter((entry) => entry.key !== line.key))} />)}<Button type="button" variant="outline" size="lg" className="w-full" onClick={() => document.getElementById("quick-order-search")?.focus()}><Plus className="h-5 w-5" />خدمة تانية</Button></Card>}
    <Card className="space-y-5 p-4 sm:p-6"><h2 className="text-lg font-black text-ink">طريقة الدفع</h2><FormField label="اختاري طريقة الدفع" htmlFor="quick-order-payment" required error={errors.paymentMethod}><div className="grid grid-cols-2 gap-3 sm:grid-cols-3" role="group" aria-label="طريقة الدفع">{paymentMethodsQuery.data?.content.map((method) => <Button key={method.id} type="button" size="lg" variant={String(method.id) === paymentMethodId ? "default" : "outline"} className="min-h-13 justify-center text-base" aria-pressed={String(method.id) === paymentMethodId} onClick={() => { setPaymentMethodId(String(method.id)); setErrors((current) => ({ ...current, paymentMethod: undefined })); }}>{method.displayNameArabic}</Button>)}</div></FormField>{paymentReferenceRequired && <FormField label="رقم العملية / مرجع الدفع" htmlFor="quick-order-payment-reference" required error={errors.paymentReference}><Input id="quick-order-payment-reference" value={paymentReference} onChange={(event) => setPaymentReference(event.target.value)} className="min-h-13 text-base" dir="ltr" autoComplete="off" maxLength={200} /></FormField>}<div className="rounded-2xl bg-primary-soft p-5"><p className="text-sm font-black text-primary">الإجمالي</p><p className="mt-2 text-3xl font-black tabular-nums text-primary">{formatMoney(total)}</p></div><FormServerError message={serverError} /><Button type="button" size="lg" className="min-h-16 w-full text-lg" disabled={mutation.isPending || Boolean(dependencyError)} onClick={submit}><Save className="h-5 w-5" />{mutation.isPending ? "جارٍ حفظ الأوردر..." : "حفظ الأوردر"}</Button></Card>
  </div>;
}

function OrderLineEditor({ line, item, employees, hairConfigurations, hairConfigurationError, hairPreview, total, onChange, onRemove }: {
  line: OrderLine;
  item: CatalogItem | undefined;
  employees: Array<{ id: number; name: string }>;
  hairConfigurations: HairConfiguration[];
  hairConfigurationError?: string;
  hairPreview: HairCalculationPreview | undefined;
  total: number;
  onChange: (key: string, change: Partial<OrderLine>) => void;
  onRemove: () => void;
}) {
  if (!item) return null;
  const hair = item.itemType === "HAIR_PRODUCT";
  const configurations = hairConfigurations.filter((configuration) => configuration.hairItemId === item.id);
  const selectedConfiguration = configurations.find((configuration) => String(configuration.id) === line.hairConfigurationId);
  const quantity = hair ? line.hairGrams : line.quantity;
  const grams = positiveWholeNumber(line.hairGrams);
  const availableStockGrams = selectedConfiguration?.availableStockGrams;
  const belowMinimum = hair && selectedConfiguration?.minimumQuantity != null
    && grams > 0 && grams < Number(selectedConfiguration.minimumQuantity);
  const stockUnconfigured = hair && selectedConfiguration !== undefined && !selectedConfiguration.stockConfigured;
  const insufficientStock = hair && availableStockGrams != null
    && grams > Number(availableStockGrams);
  const hairQuantityError = hasFractionalQuantity(quantity)
    ? "الجرامات لازم تكون رقم صحيح"
    : belowMinimum
    ? `الحد الأدنى ${selectedConfiguration?.minimumQuantity} جرام`
    : stockUnconfigured
      ? "مخزون هذا الشعر لم يتم تحديده من الأونر بعد"
      : insufficientStock
        ? "الكمية أكبر من المخزون المتاح"
        : undefined;
  const changeQuantity = (value: number) => onChange(line.key, hair ? { hairGrams: String(value) } : { quantity: String(value) });
  return <section className="rounded-2xl border border-soft-border bg-surface-soft p-4"><div className="flex items-start justify-between gap-3"><div><h3 className="font-black text-ink">{item.nameArabic}</h3><p className="mt-1 text-sm font-bold text-primary">{hair ? hairPreview ? `السعر المحسوب ${formatMoney(hairPreview.finalAmount)}` : "أدخل الجرام لاحتساب السعر" : formatMoney(total)}</p></div><Button type="button" variant="ghost" size="icon" className="shrink-0 text-danger hover:bg-danger/10 hover:text-danger" aria-label={`حذف ${item.nameArabic}`} onClick={onRemove}><Trash2 className="h-5 w-5" /></Button></div><div className="mt-4 grid gap-4 sm:grid-cols-2"><FormField label={hair ? "الجرامات" : "الكمية"} htmlFor={`quantity-${line.key}`} required error={hairQuantityError}><div className="flex items-center gap-2"><Button type="button" variant="outline" size="icon" aria-label="تقليل الكمية" onClick={() => changeQuantity(Math.max(1, positiveWholeNumber(quantity) - 1))}><Minus className="h-4 w-4" /></Button><Input id={`quantity-${line.key}`} value={quantity} type="number" min="1" step="1" inputMode="numeric" onChange={(event) => onChange(line.key, hair ? { hairGrams: event.target.value } : { quantity: event.target.value })} className="text-center font-black" /><Button type="button" variant="outline" size="icon" aria-label="زيادة الكمية" onClick={() => changeQuantity(Math.max(1, positiveWholeNumber(quantity)) + 1)}><Plus className="h-4 w-4" /></Button></div></FormField><FormField label="الموظف أو المتخصص" htmlFor={`employee-${line.key}`}><Select id={`employee-${line.key}`} value={line.employeeId} onChange={(event) => onChange(line.key, { employeeId: event.target.value })}><option value="">غير محدد</option>{employees.map((employee) => <option key={employee.id} value={employee.id}>{employee.name}</option>)}</Select></FormField>{hair && <FormField label="نوع الشعر" htmlFor={`hair-configuration-${line.key}`} required error={hairConfigurationError}><Select id={`hair-configuration-${line.key}`} value={line.hairConfigurationId} onChange={(event) => onChange(line.key, { hairConfigurationId: event.target.value })}><option value="">اختر نوع الشعر</option>{configurations.map((configuration) => <option key={configuration.id} value={configuration.id}>{[configuration.brand, configuration.length, configuration.color].filter(Boolean).join(" - ")}</option>)}</Select></FormField>}</div>{hair && selectedConfiguration && <div className="mt-4 grid gap-2 rounded-xl border border-soft-border bg-white p-3 text-sm sm:grid-cols-2"><p><span className="text-muted">الحد الأدنى: </span><bdi dir="ltr" className="font-black">{selectedConfiguration.minimumQuantity === null ? "غير محدد" : `${formatQuantity(selectedConfiguration.minimumQuantity)} جرام`}</bdi></p><p><span className="text-muted">المتاح: </span><bdi dir="ltr" className="font-black">{selectedConfiguration.stockConfigured ? `${formatQuantity(selectedConfiguration.availableStockGrams)} جرام` : "لم يتم تحديده"}</bdi></p><p><span className="text-muted">سعر الجرام: </span><bdi dir="ltr" className="font-black">{formatMoney(selectedConfiguration.unitSellingPrice)}</bdi></p><p><span className="text-muted">رسوم التركيب: </span><bdi dir="ltr" className="font-black">{formatMoney(selectedConfiguration.installationFee)}</bdi></p>{hairPreview && <><p><span className="text-muted">سعر الشعر: </span><bdi dir="ltr" className="font-black">{formatMoney(hairPreview.baseProductAmount)}</bdi></p><p><span className="text-muted">إجمالي الشعر: </span><bdi dir="ltr" className="font-black text-primary">{formatMoney(hairPreview.finalAmount)}</bdi></p></>}</div>}<bdi dir="ltr" className="mt-4 block text-end text-lg font-black tabular-nums text-ink">{formatMoney(total)}</bdi></section>;
}
