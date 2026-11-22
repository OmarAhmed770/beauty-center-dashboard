import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { getSalesReport, reportKeys } from "@/features/reports/api";
import { ReportPageHeader } from "@/features/reports/report-page-header";
import { invoiceStatusLabels } from "@/features/sales/model";
import type { InvoiceStatus } from "@/features/sales/types";
import { formatArabicDate, formatEgp } from "@/lib/format";

export function SalesReportPage() {
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("CURRENT_MONTH"));
  const [status, setStatus] = useState<InvoiceStatus | "">("");
  const [query, setQuery] = useState("");
  const params = { fromDate: range.from, toDate: range.to, status: status || undefined, query: query.trim() || undefined, page: 0, size: 500 };
  const report = useQuery({ queryKey: reportKeys.detail("sales", params), queryFn: () => getSalesReport(params) });
  const total = report.data?.content.filter((row) => row.status !== "DRAFT" && row.status !== "CANCELLED").reduce((sum, row) => sum + Number(row.finalTotal), 0) ?? 0;
  return <div className="space-y-6"><ReportPageHeader title="تقرير المبيعات" description="لقطات الفواتير التاريخية وحالاتها وخصوماتها والمبالغ المستردة." /><DateRangeFilter value={range} onChange={setRange} /><Card className="grid gap-3 p-4 md:grid-cols-[240px_1fr]"><Select aria-label="حالة الفاتورة" value={status} onChange={(event) => setStatus(event.target.value as InvoiceStatus | "")}><option value="">كل الحالات</option>{Object.entries(invoiceStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</Select><Input aria-label="بحث الفواتير" placeholder="رقم الفاتورة أو العميل أو الهاتف" value={query} onChange={(event) => setQuery(event.target.value)} /></Card>{report.isPending ? <LoadingState /> : report.isError ? <ErrorState error={report.error} onRetry={() => void report.refetch()} /> : report.data.content.length === 0 ? <EmptyState /> : <PrintableViewWrapper title="طباعة تقرير المبيعات"><article className="space-y-5" dir="rtl"><header className="border-b border-stone-200 pb-4"><h2 className="text-2xl font-black">تقرير المبيعات</h2><p className="mt-2 text-sm text-stone-500">{formatArabicDate(range.from)} إلى {formatArabicDate(range.to)}</p><p className="mt-2 font-black">إجمالي الفواتير المالية المعروضة: {formatEgp(total)}</p></header><DataTableWrapper label="الفواتير"><thead className={tableStyles.head}><tr><th>الرقم</th><th>التاريخ</th><th>العميل</th><th>قبل الخصم</th><th>الخصم</th><th>النهائي</th><th>المسترد</th><th>الحالة</th></tr></thead><tbody className={tableStyles.body}>{report.data.content.map((row) => <tr key={row.id}><td className="font-mono" dir="ltr">{row.invoiceNumber}</td><td>{formatArabicDate(row.businessDate)}</td><td>{row.customerName || "عميل نقدي"}</td><td>{formatEgp(row.subtotal)}</td><td>{formatEgp(row.discountTotal)}</td><td className="font-black">{formatEgp(row.finalTotal)}</td><td>{formatEgp(row.refundedTotal)}</td><td>{invoiceStatusLabels[row.status]}</td></tr>)}</tbody></DataTableWrapper>{report.data.totalElements > report.data.content.length && <p className="text-xs font-bold text-amber-800">يعرض التقرير أول 500 سجل؛ ضيّق الفترة لعرض البقية.</p>}</article></PrintableViewWrapper>}</div>;
}
