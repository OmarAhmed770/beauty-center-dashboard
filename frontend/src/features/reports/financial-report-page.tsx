import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { FinancialSummaryView } from "@/features/reports/financial-summary-view";
import { getFinancialReport, reportKeys } from "@/features/reports/api";
import { ReportPageHeader } from "@/features/reports/report-page-header";
import { formatArabicDate } from "@/lib/format";

export function FinancialReportPage() {
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("CURRENT_MONTH"));
  const params = { fromDate: range.from, toDate: range.to };
  const query = useQuery({ queryKey: reportKeys.financial(params), queryFn: () => getFinancialReport(params), enabled: Boolean(range.from && range.to) });
  return <div className="space-y-6"><ReportPageHeader title="التقرير المالي لفترة" description="المبيعات والمصروفات والتكاليف والربح والنقدية مع التفصيلات التشغيلية." /><DateRangeFilter value={range} onChange={setRange} />{query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : <PrintableViewWrapper title="طباعة التقرير المالي"><article className="space-y-6" dir="rtl"><header className="border-b border-stone-200 pb-4"><p className="text-xs font-black text-rose-700">مركز التجميل</p><h2 className="mt-2 text-2xl font-black">التقرير المالي</h2><p className="mt-2 text-sm text-stone-500">{formatArabicDate(query.data.fromDate)} إلى {formatArabicDate(query.data.toDate)}</p></header><FinancialSummaryView summary={query.data} compact /></article></PrintableViewWrapper>}</div>;
}
