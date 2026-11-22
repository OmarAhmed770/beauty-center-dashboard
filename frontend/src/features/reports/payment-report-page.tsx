import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { getPaymentMethodReport, reportKeys } from "@/features/reports/api";
import { ReportPageHeader } from "@/features/reports/report-page-header";
import { formatArabicDate, formatEgp } from "@/lib/format";

export function PaymentReportPage() {
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("CURRENT_MONTH"));
  const params = { fromDate: range.from, toDate: range.to };
  const query = useQuery({ queryKey: reportKeys.detail("payments", params), queryFn: () => getPaymentMethodReport(params) });
  return <div className="space-y-6"><ReportPageHeader title="تقرير طرق الدفع" description="المحصل والمسترد وصافي الحركة لكل طريقة دفع محفوظة تاريخيًا." /><DateRangeFilter value={range} onChange={setRange} />{query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : query.data.length === 0 ? <EmptyState /> : <PrintableViewWrapper title="طباعة تقرير طرق الدفع"><article className="space-y-5" dir="rtl"><header className="border-b border-stone-200 pb-4"><h2 className="text-2xl font-black">تقرير طرق الدفع</h2><p className="mt-2 text-sm text-stone-500">{formatArabicDate(range.from)} إلى {formatArabicDate(range.to)}</p></header><DataTableWrapper label="طرق الدفع"><thead className={tableStyles.head}><tr><th>الطريقة</th><th>النوع</th><th>المحصل</th><th>المسترد</th><th>الصافي</th></tr></thead><tbody className={tableStyles.body}>{query.data.map((row) => <tr key={`${row.paymentMethodId}-${row.code}`}><td><strong>{row.displayNameArabic}</strong><small className="mt-1 block font-mono" dir="ltr">{row.code}</small></td><td>{row.type === "CASH" ? "نقدي" : row.type === "CARD" ? "بطاقة" : row.type === "WALLET" ? "محفظة" : "أخرى"}</td><td>{formatEgp(row.receivedAmount)}</td><td>{formatEgp(row.refundedAmount)}</td><td className="font-black">{formatEgp(row.netAmount)}</td></tr>)}</tbody></DataTableWrapper></article></PrintableViewWrapper>}</div>;
}
