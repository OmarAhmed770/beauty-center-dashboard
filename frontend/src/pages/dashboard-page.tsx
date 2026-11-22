import { Banknote, CalendarCheck, CreditCard, ReceiptText, Scissors, UsersRound, WalletCards } from "lucide-react";
import { useQueries, useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { cairoDateIso } from "@/components/shared/date-range-filter";
import { useAuth } from "@/features/auth/auth-context";
import { closingKeys, getDailyClosingPreview } from "@/features/closing/api";
import { hairKeys, searchHairConfigurations } from "@/features/hair/api";
import { getDashboard, reportKeys } from "@/features/reports/api";
import type { FinancialSummary } from "@/features/reports/types";
import { getInvoice, invoiceKeys, searchInvoices } from "@/features/sales/api";
import type { InvoiceDetails, InvoiceSummary } from "@/features/sales/types";
import { getPendingRegistrations, userKeys } from "@/features/users/api";
import { formatArabicDate, formatArabicDateTime, formatMoney, formatNumber } from "@/lib/format";

const today = cairoDateIso();

function iso(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function previousMonthRange(date: string): { fromDate: string; toDate: string } {
  const [year, month] = date.slice(0, 7).split("-").map(Number);
  return {
    fromDate: iso(new Date(Date.UTC(year, month - 2, 1))),
    toDate: iso(new Date(Date.UTC(year, month - 1, 0))),
  };
}

/** Sums server-calculated net payment-method totals; it does not recalculate sales. */
function electronicPayments(summary: FinancialSummary): number {
  return (summary.paymentTotalsByMethod ?? [])
    .filter((method) => method.type !== "CASH")
    .reduce((total, method) => total + Number(method.netAmount), 0);
}

export function DashboardPage() {
  const { hasAnyRole } = useAuth();
  const owner = hasAnyRole(["OWNER"]);
  const priorMonth = previousMonthRange(today);
  const summaryQuery = useQuery({ queryKey: reportKeys.dashboard({ filter: "TODAY" }), queryFn: () => getDashboard({ filter: "TODAY" }) });
  const monthQuery = useQuery({ queryKey: reportKeys.dashboard({ filter: "CURRENT_MONTH" }), queryFn: () => getDashboard({ filter: "CURRENT_MONTH" }) });
  const priorMonthQuery = useQuery({ queryKey: reportKeys.dashboard({ filter: "CUSTOM", ...priorMonth }), queryFn: () => getDashboard({ filter: "CUSTOM", ...priorMonth }) });
  const ordersQuery = useQuery({ queryKey: invoiceKeys.list({ status: "COMPLETED", fromDate: today, toDate: today, page: 0, size: 5 }), queryFn: () => searchInvoices({ status: "COMPLETED", fromDate: today, toDate: today, page: 0, size: 5 }) });
  const closingQuery = useQuery({ queryKey: closingKeys.preview(today), queryFn: () => getDailyClosingPreview(today), staleTime: 60_000 });
  const hairQuery = useQuery({ queryKey: hairKeys.list({ active: true, dashboard: true }), queryFn: () => searchHairConfigurations({ active: true, effectiveOn: today, page: 0, size: 100 }), staleTime: 60_000 });
  const pendingQuery = useQuery({ queryKey: userKeys.pending, queryFn: getPendingRegistrations, enabled: owner, staleTime: 60_000 });
  const detailQueries = useQueries({ queries: (ordersQuery.data?.content ?? []).map((invoice) => ({ queryKey: invoiceKeys.detail(invoice.id), queryFn: () => getInvoice(invoice.id), staleTime: 60_000 })) });
  const detailsByInvoiceId = new Map<number, InvoiceDetails>();
  detailQueries.forEach((query) => { if (query.data) detailsByInvoiceId.set(query.data.id, query.data); });

  if (summaryQuery.isPending || ordersQuery.isPending) return <LoadingState label="جارٍ تحميل ملخص اليوم..." />;
  if (summaryQuery.isError) return <ErrorState error={summaryQuery.error} onRetry={() => void summaryQuery.refetch()} />;
  if (ordersQuery.isError) return <ErrorState error={ordersQuery.error} onRetry={() => void ordersQuery.refetch()} />;

  const summary = summaryQuery.data;
  const pendingCount = owner && pendingQuery.isSuccess ? pendingQuery.data.length : 0;
  const lowStock = (hairQuery.data?.content ?? []).filter((configuration) => configuration.stockConfigured && configuration.minimumQuantity !== null && Number(configuration.availableStockGrams) < Number(configuration.minimumQuantity));
  const monthProfitDifference = monthQuery.isSuccess && priorMonthQuery.isSuccess ? Number(monthQuery.data.netProfit) - Number(priorMonthQuery.data.netProfit) : null;

  return <div className="space-y-7 bg-white pb-4">
    <section><h1 className="text-2xl font-black tracking-tight text-ink sm:text-3xl">النهارده</h1><p className="mt-1 text-sm text-muted">{formatArabicDate(today)}</p></section>
    {pendingCount > 0 && <Banner><span>في {formatNumber(pendingCount)} طلبات تسجيل منتظرة</span><Link className="shrink-0 text-sm font-black text-primary underline underline-offset-4" to="/users">راجع الطلبات</Link></Banner>}

    <section className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6" aria-label="أداء اليوم">
      <Metric icon={ReceiptText} label="مبيعات النهارده" value={summary.grossSales} />
      <Metric icon={WalletCards} label="المصروفات" value={summary.expenses} />
      <Metric icon={Banknote} label="صافي الربح" value={summary.netProfit} tone={Number(summary.netProfit) >= 0 ? "text-success" : "text-danger"} />
      <Metric icon={ReceiptText} label="عدد الأوردرات" value={summary.completedInvoiceCount} numeric />
      <Metric icon={Banknote} label="الكاش المتوقع" value={summary.expectedCashBalance} />
      <Metric icon={CreditCard} label="المدفوعات الإلكترونية" value={electronicPayments(summary)} />
    </section>

    <section className="rounded-2xl border border-soft-border bg-surface-soft p-4 sm:p-5" aria-labelledby="month-performance-title">
      <h2 id="month-performance-title" className="text-base font-black text-ink">الشهر ده</h2>
      {monthQuery.isSuccess ? <div className="mt-4 grid gap-3 sm:grid-cols-3"><InlineMetric label="إجمالي المبيعات" value={monthQuery.data.grossSales} /><InlineMetric label="إجمالي المصروفات" value={monthQuery.data.expenses} /><InlineMetric label="صافي الربح" value={monthQuery.data.netProfit} /></div> : <p className="mt-3 text-sm font-bold text-muted">جارٍ تحميل ملخص الشهر...</p>}
      {monthProfitDifference !== null && <p className={`mt-4 text-sm font-bold ${monthProfitDifference >= 0 ? "text-success" : "text-danger"}`}>مقارنة بالشهر السابق: <bdi dir="ltr">{monthProfitDifference >= 0 ? "+" : ""}{formatMoney(monthProfitDifference)}</bdi></p>}
    </section>

    <section className="grid gap-3 md:grid-cols-3" aria-label="تنبيهات مفيدة">
      <Insight title="أكثر خدمة مبيعًا" value={summary.topServicesAndProducts?.[0]?.itemNameArabic ?? "لا توجد مبيعات كافية"} to="/sales" />
      <Insight title="مخزون الشعر" value={lowStock.length ? `${formatNumber(lowStock.length)} إعدادات تحتاج متابعة` : "المخزون مطمئن"} to="/catalog/hair" warning={lowStock.length > 0} />
      <Insight title="الإغلاق اليومي" value={closingQuery.data?.status === "CLOSED" ? "تم إغلاق اليوم" : "اليوم لم يُغلق بعد"} to="/daily-closing" warning={closingQuery.data?.status !== "CLOSED"} />
    </section>

    <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5" aria-label="إجراءات سريعة">
      <QuickAction to="/orders/new" icon={ReceiptText} label="إضافة أوردر" /><QuickAction to="/expenses?new=1" icon={WalletCards} label="إضافة مصروف" /><QuickAction to="/catalog" icon={Scissors} label="الخدمات والأسعار" /><QuickAction to="/employees" icon={UsersRound} label="الموظفون" /><QuickAction to="/daily-closing" icon={CalendarCheck} label="الإقفال اليومي" />
    </section>

    <section aria-labelledby="latest-orders-title"><div className="mb-3 flex items-center justify-between gap-4"><h2 id="latest-orders-title" className="text-lg font-black text-ink">آخر الأوردرات</h2><Link className="text-sm font-black text-primary hover:underline" to="/sales">كل الأوردرات</Link></div>{ordersQuery.data.content.length === 0 ? <EmptyState title="لا توجد أوردرات مكتملة اليوم" /> : <ul className="divide-y divide-soft-border border-y border-soft-border">{ordersQuery.data.content.map((order) => <LatestOrder key={order.id} order={order} detail={detailsByInvoiceId.get(order.id)} />)}</ul>}</section>
  </div>;
}

function Banner({ children }: { children: ReactNode }) { return <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-primary-soft px-4 py-3 text-sm font-bold text-ink">{children}</div>; }
function Metric({ icon: Icon, label, value, numeric = false, tone = "text-ink" }: { icon: typeof ReceiptText; label: string; value: number | string; numeric?: boolean; tone?: string }) { return <div className="min-w-0 rounded-2xl border border-soft-border bg-white p-4 shadow-sm"><Icon className="h-5 w-5 text-primary" aria-hidden="true" /><p className="mt-4 text-xs font-bold text-muted">{label}</p><bdi dir="ltr" className={`mt-2 block truncate text-xl font-black tabular-nums sm:text-2xl ${tone}`}>{numeric ? formatNumber(value) : formatMoney(value)}</bdi></div>; }
function InlineMetric({ label, value }: { label: string; value: number | string }) { return <div><p className="text-xs font-bold text-muted">{label}</p><bdi dir="ltr" className="mt-1 block text-lg font-black tabular-nums text-ink">{formatMoney(value)}</bdi></div>; }
function Insight({ title, value, to, warning = false }: { title: string; value: string; to: string; warning?: boolean }) { return <Link to={to} className="rounded-2xl border border-soft-border bg-white p-4 transition-colors hover:bg-surface-soft"><p className="text-xs font-bold text-muted">{title}</p><p className={`mt-2 font-black ${warning ? "text-danger" : "text-ink"}`}>{value}</p></Link>; }
function QuickAction({ to, icon: Icon, label }: { to: string; icon: typeof ReceiptText; label: string }) { return <Link to={to} className="flex min-h-16 items-center justify-center gap-2 rounded-2xl border border-soft-border bg-white px-4 text-sm font-black text-ink transition-colors hover:border-primary hover:bg-primary-soft"><Icon className="h-5 w-5 text-primary" aria-hidden="true" />{label}</Link>; }
function LatestOrder({ order, detail }: { order: InvoiceSummary; detail?: InvoiceDetails }) { const services = detail?.items.map((item) => item.itemNameArabic).filter(Boolean).join("، "); const employees = Array.from(new Set(detail?.items.map((item) => item.employeeName).filter((name): name is string => Boolean(name)) ?? [])).join("، "); const payment = detail?.payments.map((entry) => entry.paymentMethodNameArabic).join("، "); const title = services || order.customerName || "أوردر بدون اسم عميلة"; const secondary = [formatArabicDateTime(order.completedAt || order.createdAt), employees, payment].filter(Boolean).join(" · "); return <li><Link to={`/sales/invoices/${order.id}`} className="flex min-w-0 items-center justify-between gap-4 px-1 py-4 transition-colors hover:bg-surface-soft sm:px-3"><div className="min-w-0"><p className="truncate font-black text-ink">{title}</p><p className="mt-1 truncate text-xs text-muted">{secondary}</p></div><bdi dir="ltr" className="shrink-0 font-black tabular-nums text-ink">{formatMoney(order.finalTotal)}</bdi></Link></li>; }
