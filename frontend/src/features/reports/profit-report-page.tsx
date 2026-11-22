import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { Card } from "@/components/ui/card";
import { getProfitReport, reportKeys } from "@/features/reports/api";
import { ReportPageHeader } from "@/features/reports/report-page-header";
import { formatArabicDate, formatEgp } from "@/lib/format";

export function ProfitReportPage() {
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("CURRENT_MONTH"));
  const params = { fromDate: range.from, toDate: range.to };
  const query = useQuery({ queryKey: reportKeys.detail("profit", params), queryFn: () => getProfitReport(params) });
  return <div className="space-y-6"><ReportPageHeader title="تقرير صافي الربح" description="صافي المبيعات ناقص تكلفة المنتجات والشعر والمصروفات التشغيلية وتكلفة الموظفين." /><DateRangeFilter value={range} onChange={setRange} />{query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : <PrintableViewWrapper title="طباعة تقرير الربح"><article className="space-y-6" dir="rtl"><header className="border-b border-stone-200 pb-4"><h2 className="text-2xl font-black">تقرير صافي الربح</h2><p className="mt-2 text-sm text-stone-500">{formatArabicDate(query.data.fromDate)} إلى {formatArabicDate(query.data.toDate)}</p></header><div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5"><Value label="صافي المبيعات" value={query.data.netSales} /><Value label="تكلفة المباع" value={query.data.costOfSoldItemsAndHair} negative /><Value label="المصروفات التشغيلية" value={query.data.operatingExpenses} negative /><Value label="تكلفة الموظفين" value={query.data.employeeCost} negative /><Value label="صافي الربح" value={query.data.netProfit} accent /></div><Card className="border-sky-200 bg-sky-50 p-4 text-sm leading-7 text-sky-950">السلف ومدفوعات الموظفين تقلل الرصيد المستحق والنقدية، ولا تُخصم مرة ثانية من صافي الربح. المبالغ المستردة تقلل صافي المبيعات.</Card></article></PrintableViewWrapper>}</div>;
}

function Value({ label, value, negative = false, accent = false }: { label: string; value: number | string; negative?: boolean; accent?: boolean }) { return <div className={`rounded-xl p-4 ${accent ? "bg-emerald-700 text-white" : "bg-stone-50"}`}><p className={`text-xs font-bold ${accent ? "text-emerald-100" : "text-stone-500"}`}>{label}</p><p className={`mt-2 text-lg font-black ${negative ? "text-red-700" : ""}`}>{negative ? "− " : ""}{formatEgp(value)}</p></div>; }
