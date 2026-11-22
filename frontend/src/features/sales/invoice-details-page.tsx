import { ArrowRight, Pencil, XCircle } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { ReasonDialog } from "@/components/shared/reason-dialog";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";
import { useBusinessProfile } from "@/features/business/currency-context";
import { cancelInvoice, getInvoice, invoiceKeys } from "@/features/sales/api";
import { invoiceStatusLabels } from "@/features/sales/model";
import type { InvoiceDetails } from "@/features/sales/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { invalidateFinancialViews } from "@/lib/financial-query-invalidation";
import { formatArabicDate, formatArabicDateTime, formatEgp, formatQuantity } from "@/lib/format";

const statusTones = { DRAFT: "warning", COMPLETED: "success", CANCELLED: "danger", PARTIALLY_REFUNDED: "info", REFUNDED: "neutral" } as const;

export function InvoiceDetailsPage({ backPath = "/sales", backLabel = "قائمة الفواتير" }: { backPath?: string; backLabel?: string }) {
  const invoiceId = Number(useParams<{ invoiceId: string }>().invoiceId);
  const { hasPermission } = useAuth();
  const { profile } = useBusinessProfile();
  const canCancel = hasPermission("INVOICES_AND_RETURNS");
  const queryClient = useQueryClient();
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const invoiceQuery = useQuery({ queryKey: invoiceKeys.detail(invoiceId), queryFn: () => getInvoice(invoiceId), enabled: Number.isInteger(invoiceId) && invoiceId > 0 });
  const cancelMutation = useMutation({ mutationFn: (reason: string) => cancelInvoice(invoiceId, reason), onSuccess: async (updated) => { queryClient.setQueryData(invoiceKeys.detail(invoiceId), updated); await Promise.all([queryClient.invalidateQueries({ queryKey: invoiceKeys.all }), invalidateFinancialViews(queryClient)]); setCancelOpen(false); setCancelError(null); }, onError: (error) => setCancelError(getArabicApiErrorMessage(error)) });
  if (invoiceQuery.isPending) return <LoadingState label="جارٍ تحميل الفاتورة..." />;
  if (invoiceQuery.isError) return <ErrorState error={invoiceQuery.error} onRetry={() => void invoiceQuery.refetch()} />;
  const invoice = invoiceQuery.data;
  const cancelAllowedByStatus = invoice.status === "DRAFT" || invoice.status === "COMPLETED";
  return (
    <div className="space-y-6">
      <PageHeader title={`الفاتورة ${invoice.invoiceNumber}`} description={`تاريخ العمل ${formatArabicDate(invoice.businessDate)}`} actions={<><Button asChild variant="outline"><Link to={backPath}><ArrowRight className="h-4 w-4" />{backLabel}</Link></Button>{invoice.status === "DRAFT" && canCancel && <Button asChild><Link to={`/sales/invoices/${invoice.id}/edit`}><Pencil className="h-4 w-4" />فتح المسودة</Link></Button>}{canCancel && cancelAllowedByStatus && <Button variant="destructive" onClick={() => setCancelOpen(true)}><XCircle className="h-4 w-4" />إلغاء الفاتورة</Button>}</>} />
      <PrintableViewWrapper title="طباعة الفاتورة" allowReceipt>
        <article className="space-y-7" dir="rtl">
          <header className="flex flex-col items-start justify-between gap-4 sm:flex-row sm:gap-5"><div className="min-w-0"><p className="text-xs font-black text-rose-700">{profile?.businessName ?? "مركز التجميل"}</p><h2 className="mt-2 text-2xl font-black text-stone-950">فاتورة مبيعات</h2><p className="mt-2 break-all font-mono text-sm text-stone-500" dir="ltr">{invoice.invoiceNumber}</p></div><div className="self-start sm:text-left"><StatusBadge tone={statusTones[invoice.status]}>{invoiceStatusLabels[invoice.status]}</StatusBadge><p className="mt-3 text-sm font-bold text-stone-700">{formatArabicDate(invoice.businessDate)}</p></div></header>
          <section className="grid gap-4 rounded-xl bg-stone-50 p-4 sm:grid-cols-3"><Info label="العميل" value={invoice.customerName || "عميل نقدي"} /><Info label="الهاتف" value={invoice.customerPhone || "—"} /><Info label="منشئ الفاتورة" value={invoice.createdBy || "النظام"} /></section>
          {invoice.notes && <section><h3 className="text-sm font-black text-stone-900">ملاحظات</h3><p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-stone-600">{invoice.notes}</p></section>}
          <div className="space-y-3 md:hidden print:hidden"><MobileInvoiceItems items={invoice.items} /></div>
          <DataTableWrapper className="hidden md:block print:block" label="بنود الفاتورة"><thead className={tableStyles.head}><tr><th>البند</th><th>الكمية</th><th>السعر الأصلي</th><th>الخصم</th><th>الإجمالي</th></tr></thead><tbody className={tableStyles.body}>{invoice.items.map((item) => <tr key={item.id}><td><p className="font-black text-stone-950">{item.itemNameArabic}</p>{item.employeeName && <p className="mt-1 text-xs text-stone-400">المنفذ: {item.employeeName}</p>}{item.priceOverridden && <p className="mt-1 text-xs font-bold text-amber-700">سعر يدوي مصرح</p>}</td><td><bdi dir="ltr">{formatQuantity(item.quantity)}</bdi></td><td>{formatEgp(item.lineSubtotal)}</td><td>{formatEgp(item.discountAmount)}{item.discountName && <small className="mt-1 block text-stone-400">{item.discountName}</small>}</td><td className="font-black">{formatEgp(item.finalLineTotal)}</td></tr>)}</tbody></DataTableWrapper>
          <section className="ms-auto grid max-w-xl gap-3 sm:grid-cols-3"><Total label="قبل الخصم" value={invoice.subtotal} /><Total label="الخصومات" value={invoice.discountTotal} /><Total label="الإجمالي النهائي" value={invoice.finalTotal} accent /></section>
          <section><h3 className="text-lg font-black text-stone-950">الدفعات</h3>{invoice.payments.length === 0 ? <p className="mt-3 text-sm text-stone-500">لا توجد دفعات محفوظة.</p> : <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{invoice.payments.map((payment) => <div key={payment.id} className="rounded-xl border border-stone-200 p-4"><p className="font-black text-stone-900">{payment.paymentMethodNameArabic}</p><p className="mt-2 text-lg font-black tabular-nums">{formatEgp(payment.amount)}</p>{payment.referenceNumber && <p className="mt-1 break-all text-xs text-stone-400" dir="ltr">{payment.referenceNumber}</p>}</div>)}</div>}</section>
          {invoice.refunds.length > 0 && <section><h3 className="text-lg font-black text-stone-950">المبالغ المستردة</h3><div className="mt-3 space-y-2">{invoice.refunds.map((refund) => <div key={refund.id} className="flex flex-col gap-2 rounded-xl border border-amber-200 bg-amber-50 p-4 sm:flex-row sm:justify-between"><span><strong className="block text-amber-950">{formatEgp(refund.amount)}</strong><small className="text-amber-800">{refund.reason}</small></span><span className="text-sm font-bold text-amber-900">{formatArabicDate(refund.businessDate)}</span></div>)}</div></section>}
          {invoice.status === "CANCELLED" && <section className="rounded-xl border border-red-200 bg-red-50 p-4"><h3 className="font-black text-red-950">الفاتورة ملغاة</h3><p className="mt-2 text-sm text-red-800">{invoice.cancellationReason}</p><p className="mt-2 text-xs text-red-700">بواسطة {invoice.cancelledBy || "النظام"} — {invoice.cancelledAt ? formatArabicDateTime(invoice.cancelledAt) : "—"}</p></section>}
          <footer className="border-t border-stone-200 pt-4 text-xs text-stone-400"><p>أُنشئت: {formatArabicDateTime(invoice.createdAt)}</p>{invoice.completedAt && <p className="mt-1">اكتملت: {formatArabicDateTime(invoice.completedAt)} بواسطة {invoice.completedBy}</p>}</footer>
        </article>
      </PrintableViewWrapper>
      <ReasonDialog open={cancelOpen} title="إلغاء الفاتورة" description="لن تُحذف الفاتورة أو بنودها أو دفعاتها، وسيُحفظ سبب الإلغاء في سجل التدقيق." busy={cancelMutation.isPending} error={cancelError} onOpenChange={(open) => { setCancelOpen(open); if (!open) setCancelError(null); }} onConfirm={(reason) => cancelMutation.mutate(reason)} />
    </div>
  );
}

function MobileInvoiceItems({ items }: { items: InvoiceDetails["items"] }) {
  return items.map((item) => (
    <section key={item.id} className="rounded-xl border border-stone-200 bg-white p-4">
      <p className="font-black text-stone-950">{item.itemNameArabic}</p>
      {item.employeeName && <p className="mt-1 text-xs text-stone-500">المنفذ: {item.employeeName}</p>}
      {item.priceOverridden && <p className="mt-1 text-xs font-bold text-amber-700">سعر يدوي مصرح</p>}
      <div className="mt-3 grid grid-cols-2 gap-3 border-t border-stone-100 pt-3 text-sm"><div><p className="text-xs font-bold text-stone-400">الكمية</p><p className="mt-1 font-bold tabular-nums"><bdi dir="ltr">{formatQuantity(item.quantity)}</bdi></p></div><div><p className="text-xs font-bold text-stone-400">قبل الخصم</p><p className="mt-1 font-bold tabular-nums">{formatEgp(item.lineSubtotal)}</p></div><div><p className="text-xs font-bold text-stone-400">الخصم</p><p className="mt-1 font-bold tabular-nums text-red-700">{formatEgp(item.discountAmount)}</p>{item.discountName && <p className="mt-1 text-xs text-stone-400">{item.discountName}</p>}</div><div><p className="text-xs font-bold text-stone-400">الإجمالي</p><p className="mt-1 font-black tabular-nums text-rose-800">{formatEgp(item.finalLineTotal)}</p></div></div>
    </section>
  ));
}

function Info({ label, value }: { label: string; value: string }) { return <div><dt className="text-xs font-bold text-stone-400">{label}</dt><dd className="mt-1 font-black text-stone-900">{value}</dd></div>; }
function Total({ label, value, accent = false }: { label: string; value: number | string; accent?: boolean }) { return <div className={accent ? "rounded-xl bg-rose-700 p-4 text-white" : "rounded-xl bg-stone-50 p-4"}><p className={accent ? "text-xs font-bold text-rose-100" : "text-xs font-bold text-stone-500"}>{label}</p><p className="mt-2 text-xl font-black tabular-nums">{formatEgp(value)}</p></div>; }
