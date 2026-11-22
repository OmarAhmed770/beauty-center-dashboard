import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { cairoDateIso } from "@/components/shared/date-range-filter";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { DailyClosingReportView } from "@/features/closing/daily-closing-report-view";
import { closingKeys, getLatestDailyClosing } from "@/features/closing/api";
import { getDailyClosingReport, reportKeys } from "@/features/reports/api";
import { ReportPageHeader } from "@/features/reports/report-page-header";
import { demoReadOnly } from "@/lib/demo-mode";

export function DailyReportPage() {
  const [selectedDate, setDate] = useState<string | undefined>(() => demoReadOnly ? undefined : cairoDateIso());
  const latest = useQuery({ queryKey: closingKeys.latest, queryFn: getLatestDailyClosing, enabled: demoReadOnly, retry: false });
  const date = selectedDate ?? latest.data?.businessDate ?? "";
  const query = useQuery({ queryKey: reportKeys.detail("daily", { date }), queryFn: () => getDailyClosingReport(date), enabled: Boolean(date), retry: false });
  return <div className="space-y-6">
    <ReportPageHeader title="التقرير اليومي" description="يعرض لقطة الإغلاق المحفوظة؛ لا يعيد احتساب يوم تاريخي عند الطباعة." />
    <Card className="p-5"><label className="block max-w-sm text-sm font-bold">تاريخ العمل<Input className="mt-2" type="date" value={date} onChange={(event) => setDate(event.target.value)} /></label>
      {demoReadOnly && selectedDate === undefined && date && <p className="mt-3 text-sm text-muted">الديمو يعرض آخر يوم له تقرير إغلاق محفوظ. يمكنك اختيار تاريخ آخر.</p>}
    </Card>
    {demoReadOnly && selectedDate === undefined && latest.isPending ? <LoadingState />
      : demoReadOnly && selectedDate === undefined && latest.isError ? <ErrorState error={latest.error} onRetry={() => void latest.refetch()} />
        : !date ? <EmptyState title="لا توجد لقطة إغلاق محفوظة" description="اختر تاريخًا له تقرير إغلاق محفوظ لعرضه." />
          : query.isPending ? <LoadingState />
            : query.isError ? <ErrorState title="لا توجد لقطة إغلاق لهذا التاريخ" error={query.error} onRetry={() => void query.refetch()} />
              : <PrintableViewWrapper title="طباعة التقرير اليومي" allowReceipt><DailyClosingReportView closing={query.data} /></PrintableViewWrapper>}
  </div>;
}
