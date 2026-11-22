import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, ReceiptText } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { cairoDateIso } from "@/components/shared/date-range-filter";
import { getInvoice, invoiceKeys, searchInvoices } from "@/features/sales/api";
import { invoiceStatusLabels } from "@/features/sales/model";
import type { InvoiceDetails, InvoiceSummary } from "@/features/sales/types";
import { formatArabicDateTime, formatMoney } from "@/lib/format";

const statusTones = { DRAFT: "warning", COMPLETED: "success", CANCELLED: "danger", PARTIALLY_REFUNDED: "info", REFUNDED: "neutral" } as const;

export function TodayOrdersPage() {
  const today = cairoDateIso();
  const ordersQuery = useQuery({
    queryKey: invoiceKeys.list({ fromDate: today, toDate: today, page: 0, size: 30, own: true }),
    queryFn: () => searchInvoices({ fromDate: today, toDate: today, page: 0, size: 30 }),
  });
  if (ordersQuery.isPending) return <LoadingState label="جارٍ تحميل أوردرات النهارده..." />;
  if (ordersQuery.isError) return <ErrorState error={ordersQuery.error} onRetry={() => void ordersQuery.refetch()} />;
  const orders = ordersQuery.data.content;
  return <div className="mx-auto max-w-3xl space-y-5"><PageHeader title="أوردرات النهارده" description="الأوردرات التي سجلتها أنت اليوم." />{orders.length === 0 ? <EmptyState title="لسه مفيش أوردرات النهارده" description="سجل أول أوردر عشان يظهر هنا." /> : <div className="space-y-3">{orders.map((order) => <TodayOrderCard key={order.id} order={order} />)}</div>}</div>;
}

function TodayOrderCard({ order }: { order: InvoiceSummary }) {
  const detailsQuery = useQuery({ queryKey: invoiceKeys.detail(order.id), queryFn: () => getInvoice(order.id), staleTime: 60_000 });
  const details = detailsQuery.data;
  return <Link to={`/orders/today/${order.id}`} className="block rounded-2xl border border-[#eee7ea] bg-white p-4 shadow-sm transition hover:border-[#E88DB4] hover:bg-[#fffafb] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A83868]"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="truncate font-black text-[#29252A]">{details ? serviceNames(details) : order.customerName || "أوردر جديد"}</p><p className="mt-1 text-xs font-bold text-[#736873]">{formatArabicDateTime(order.completedAt || order.createdAt)}</p></div><div className="flex shrink-0 items-center gap-2"><StatusBadge tone={statusTones[order.status]}>{invoiceStatusLabels[order.status]}</StatusBadge><ChevronLeft className="h-5 w-5 text-[#A83868]" aria-hidden="true" /></div></div><div className="mt-4 grid grid-cols-2 gap-3 border-t border-[#f1ebee] pt-3 text-sm"><div><p className="text-xs font-bold text-[#a69aa2]">الإجمالي</p><p className="mt-1 font-black tabular-nums text-[#A83868]">{formatMoney(order.finalTotal)}</p></div><div><p className="text-xs font-bold text-[#a69aa2]">الدفع</p><p className="mt-1 truncate font-bold text-[#29252A]">{details ? details.payments.map((payment) => payment.paymentMethodNameArabic).join("، ") : "جارٍ التحميل..."}</p></div>{details && <div className="col-span-2"><p className="text-xs font-bold text-[#a69aa2]">الموظف أو المتخصص</p><p className="mt-1 truncate font-bold text-[#29252A]">{employeeNames(details) || "غير محدد"}</p></div>}</div></Link>;
}

function serviceNames(invoice: InvoiceDetails): string {
  return invoice.items.map((item) => item.itemNameArabic).join("، ") || "أوردر";
}

function employeeNames(invoice: InvoiceDetails): string {
  return Array.from(new Set(invoice.items.map((item) => item.employeeName).filter((name): name is string => Boolean(name)))).join("، ");
}
