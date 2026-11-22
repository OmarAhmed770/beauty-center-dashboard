import { CalendarCheck, LockKeyhole, RotateCcw } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { MoneyInput } from "@/components/shared/money-input";
import { PageHeader } from "@/components/shared/page-header";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { ReasonDialog } from "@/components/shared/reason-dialog";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/auth/auth-context";
import { closeDaily, closingKeys, getDailyClosingPreview, getStoredDailyClosing, reopenDaily } from "@/features/closing/api";
import { DailyClosingReportView } from "@/features/closing/daily-closing-report-view";
import { calculateCashDifference, closingStatusLabels, validateActualCash } from "@/features/closing/model";
import { cairoDateIso } from "@/components/shared/date-range-filter";
import { FinancialSummaryView } from "@/features/reports/financial-summary-view";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { formatEgp } from "@/lib/format";

const statusTone = { OPEN: "warning", CLOSED: "success", REOPENED: "info" } as const;

export function DailyClosingPage() {
  const { hasAnyRole } = useAuth();
  const owner = hasAnyRole(["OWNER"]);
  const queryClient = useQueryClient();
  const [businessDate, setBusinessDate] = useState(cairoDateIso());
  const [actualCash, setActualCash] = useState("");
  const [notes, setNotes] = useState("");
  const [confirmClose, setConfirmClose] = useState(false);
  const [reopenOpen, setReopenOpen] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const previewQuery = useQuery({ queryKey: closingKeys.preview(businessDate), queryFn: () => getDailyClosingPreview(businessDate), enabled: Boolean(businessDate) });
  const storedQuery = useQuery({ queryKey: closingKeys.stored(businessDate), queryFn: () => getStoredDailyClosing(businessDate), enabled: previewQuery.data?.status !== "OPEN" });
  const refresh = async () => { await Promise.all([queryClient.invalidateQueries({ queryKey: closingKeys.preview(businessDate) }), queryClient.invalidateQueries({ queryKey: closingKeys.stored(businessDate) }), queryClient.invalidateQueries({ queryKey: ["reports"] })]); };
  const closeMutation = useMutation({ mutationFn: () => closeDaily(businessDate, { actualCash, notes: notes.trim() || undefined }), onSuccess: async () => { setConfirmClose(false); setActionError(null); await refresh(); }, onError: (error) => { setConfirmClose(false); setActionError(getArabicApiErrorMessage(error)); } });
  const reopenMutation = useMutation({ mutationFn: (reason: string) => reopenDaily(businessDate, reason), onSuccess: async () => { setReopenOpen(false); setActionError(null); await refresh(); }, onError: (error) => setActionError(getArabicApiErrorMessage(error)) });
  const requestClose = () => { const error = validateActualCash(actualCash); if (error) { setActionError(error); return; } setActionError(null); setConfirmClose(true); };

  return <div className="space-y-7"><PageHeader title="الإغلاق اليومي" description="المعاينة الحية للحساب، والإغلاق يحفظ لقطة ثابتة ويقفل السجلات المالية لهذا التاريخ." />
    <Card className="grid gap-4 p-5 md:grid-cols-[240px_1fr] md:items-end"><label className="text-sm font-bold text-stone-700">تاريخ العمل<Input className="mt-2" type="date" value={businessDate} onChange={(event) => { setBusinessDate(event.target.value); setActionError(null); }} /></label><p className="rounded-xl bg-amber-50 p-4 text-sm leading-7 text-amber-950">إعادة الفتح متاحة للمالك فقط وتتطلب سببًا. أي سجل جديد على تاريخ مغلق سيرفضه الخادم حتى إعادة الفتح.</p></Card>
    {previewQuery.isPending ? <LoadingState label="جارٍ احتساب معاينة الإغلاق..." /> : previewQuery.isError ? <ErrorState error={previewQuery.error} onRetry={() => void previewQuery.refetch()} /> : <>
      <div className="flex flex-wrap items-center gap-3"><StatusBadge tone={statusTone[previewQuery.data.status]}>{closingStatusLabels[previewQuery.data.status]}</StatusBadge>{previewQuery.data.status === "CLOSED" && <span className="flex items-center gap-2 text-sm font-bold text-emerald-800"><LockKeyhole className="h-4 w-4" />هذا التاريخ مقفل أمام التغييرات المالية.</span>}{owner && previewQuery.data.status === "CLOSED" && <Button variant="outline" onClick={() => setReopenOpen(true)}><RotateCcw className="h-4 w-4" />إعادة فتح اليوم</Button>}</div>
      <FinancialSummaryView summary={previewQuery.data.liveFinancials} compact />
      {previewQuery.data.status !== "CLOSED" && <Card className="p-5"><div className="mb-5 flex items-center gap-3"><CalendarCheck className="text-rose-700" /><div><h2 className="font-black">تسجيل النقدية الفعلية</h2><p className="mt-1 text-xs text-stone-500">يفرض الخادم الحسابات والصلاحيات مرة أخرى داخل معاملة الإغلاق.</p></div></div><div className="grid gap-4 lg:grid-cols-3"><label className="text-sm font-bold text-stone-700">النقدية المتوقعة<div className="mt-2 rounded-xl bg-stone-100 px-4 py-3 text-lg font-black">{formatEgp(previewQuery.data.liveFinancials.expectedCashBalance)}</div></label><label className="text-sm font-bold text-stone-700">النقدية الفعلية<MoneyInput className="mt-2" value={actualCash} onChange={(event) => setActualCash(event.target.value)} /></label><label className="text-sm font-bold text-stone-700">فرق النقدية<div className={`mt-2 rounded-xl px-4 py-3 text-lg font-black ${calculateCashDifference(actualCash, previewQuery.data.liveFinancials.expectedCashBalance) === 0 ? "bg-emerald-50 text-emerald-800" : "bg-amber-50 text-amber-900"}`}>{formatEgp(calculateCashDifference(actualCash, previewQuery.data.liveFinancials.expectedCashBalance))}</div></label><label className="text-sm font-bold text-stone-700 lg:col-span-3">ملاحظات الإغلاق<Textarea className="mt-2" maxLength={2000} value={notes} onChange={(event) => setNotes(event.target.value)} /></label></div><div className="mt-5 flex justify-end"><Button size="lg" onClick={requestClose} disabled={closeMutation.isPending}><LockKeyhole className="h-5 w-5" />إغلاق اليوم</Button></div></Card>}
      {actionError && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-800">{actionError}</div>}
      {storedQuery.isPending && previewQuery.data.status !== "OPEN" ? <LoadingState label="جارٍ تحميل لقطة الإغلاق المحفوظة..." /> : storedQuery.isError ? <ErrorState error={storedQuery.error} onRetry={() => void storedQuery.refetch()} /> : storedQuery.data ? <PrintableViewWrapper title="إعادة طباعة لقطة الإغلاق" allowReceipt><DailyClosingReportView closing={storedQuery.data} /></PrintableViewWrapper> : null}
    </>}
    <ConfirmationDialog open={confirmClose} title="تأكيد إغلاق اليوم" description="سيتم حفظ اللقطة المالية وقفل جميع الحركات المالية لهذا التاريخ. راجع النقدية الفعلية قبل المتابعة." confirmLabel="تأكيد الإغلاق" busy={closeMutation.isPending} onOpenChange={setConfirmClose} onConfirm={() => closeMutation.mutate()} />
    <ReasonDialog open={reopenOpen} title="إعادة فتح تاريخ العمل" description="سيُفتح التاريخ للحركات المالية ويُحفظ السبب والمالك والوقت في سجل التدقيق. اللقطة السابقة تبقى للطباعة حتى إعادة الإغلاق." busy={reopenMutation.isPending} error={actionError} onOpenChange={(open) => { setReopenOpen(open); if (!open) setActionError(null); }} onConfirm={(reason) => reopenMutation.mutate(reason)} />
  </div>;
}
