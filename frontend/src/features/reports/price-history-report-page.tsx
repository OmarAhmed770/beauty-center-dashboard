import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { catalogKeys, searchCatalogItems } from "@/features/catalog/api";
import { priceValueTypeLabels } from "@/features/catalog/model";
import type { PriceValueType } from "@/features/catalog/types";
import { getPriceHistoryReport, reportKeys } from "@/features/reports/api";
import { ReportPageHeader } from "@/features/reports/report-page-header";
import { formatArabicDate, formatArabicDateTime, formatEgp } from "@/lib/format";

export function PriceHistoryReportPage() {
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("CURRENT_MONTH"));
  const [itemId, setItemId] = useState(0);
  const [valueType, setValueType] = useState<PriceValueType | "">("");
  const items = useQuery({ queryKey: catalogKeys.items({ size: 100 }), queryFn: () => searchCatalogItems({ page: 0, size: 100 }) });
  const params = { itemId: itemId || undefined, valueType: valueType || undefined, fromDateTime: `${range.from}T00:00:00`, toDateTime: `${range.to}T23:59:59`, page: 0, size: 500 };
  const query = useQuery({ queryKey: reportKeys.detail("price-history", params), queryFn: () => getPriceHistoryReport(params) });
  return <div className="space-y-6"><ReportPageHeader title="تقرير سجل الأسعار" description="سجل غير قابل للتعديل لتغيرات سعر البيع وسعر الشراء مع السبب والمنفذ والوقت." /><DateRangeFilter value={range} onChange={setRange} /><Card className="grid gap-3 p-4 md:grid-cols-2"><Select aria-label="صنف الكتالوج" value={itemId || ""} onChange={(event) => setItemId(Number(event.target.value))}><option value="">كل الأصناف</option>{items.data?.content.map((row) => <option key={row.id} value={row.id}>{row.nameArabic} — {row.code}</option>)}</Select><Select aria-label="نوع القيمة" value={valueType} onChange={(event) => setValueType(event.target.value as PriceValueType | "")}><option value="">سعر البيع وسعر الشراء</option><option value="SELLING_PRICE">سعر البيع</option><option value="COST">سعر الشراء</option></Select></Card>{query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : query.data.content.length === 0 ? <EmptyState /> : <PrintableViewWrapper title="طباعة سجل الأسعار"><article className="space-y-5" dir="rtl"><header className="border-b border-stone-200 pb-4"><h2 className="text-2xl font-black">تقرير سجل الأسعار</h2><p className="mt-2 text-sm text-stone-500">{formatArabicDate(range.from)} إلى {formatArabicDate(range.to)}</p></header><DataTableWrapper label="تغييرات الأسعار"><thead className={tableStyles.head}><tr><th>الصنف</th><th>القيمة</th><th>القديم</th><th>الجديد</th><th>السبب</th><th>المستخدم والوقت</th></tr></thead><tbody className={tableStyles.body}>{query.data.content.map((row) => <tr key={row.id}><td><strong>{row.catalogItemNameArabic}</strong><small className="mt-1 block font-mono" dir="ltr">{row.catalogItemCode}</small></td><td>{priceValueTypeLabels[row.valueType]}</td><td>{formatEgp(row.oldValue)}</td><td className="font-black">{formatEgp(row.newValue)}</td><td>{row.changeReason}</td><td>{row.changedBy}<small className="mt-1 block text-stone-400">{formatArabicDateTime(row.effectiveAt)}</small></td></tr>)}</tbody></DataTableWrapper>{query.data.totalElements > query.data.content.length && <p className="text-xs font-bold text-amber-800">يعرض التقرير أول 500 تغيير؛ ضيّق الفترة لعرض البقية.</p>}</article></PrintableViewWrapper>}</div>;
}
