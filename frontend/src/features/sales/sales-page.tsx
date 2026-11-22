import { Eye, Pencil, Search, SlidersHorizontal } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { PaginationControls } from "@/components/shared/pagination-controls";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { invoiceKeys, searchInvoices } from "@/features/sales/api";
import { invoiceStatusLabels } from "@/features/sales/model";
import type { InvoiceStatus, InvoiceSummary } from "@/features/sales/types";
import { formatArabicDate, formatEgp } from "@/lib/format";

const statusTones = { DRAFT: "warning", COMPLETED: "success", CANCELLED: "danger", PARTIALLY_REFUNDED: "info", REFUNDED: "neutral" } as const;

export function SalesPage() {
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("CURRENT_MONTH"));
  const [draftQuery, setDraftQuery] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<InvoiceStatus | "">("");
  const [page, setPage] = useState(0);
  const params = { fromDate: range.from, toDate: range.to, query: query || undefined, status: status || undefined, page, size: 20 };
  const invoicesQuery = useQuery({ queryKey: invoiceKeys.list(params), queryFn: () => searchInvoices(params) });
  const submit = (event: FormEvent) => { event.preventDefault(); setQuery(draftQuery.trim()); setPage(0); };
  return (
    <div className="space-y-6">
      <PageHeader
        title="إدارة الفواتير المتقدمة"
        description="ابحث في السجل الكامل، وراجع الفواتير والمرتجعات المحفوظة."
      />
      <DateRangeFilter value={range} onChange={(next) => { setRange(next); setPage(0); }} />
      <form className="grid gap-3 rounded-[1.5rem] border border-[#eee7ea] bg-white p-4 shadow-[0_10px_30px_rgb(41_37_42_/_0.04)] md:grid-cols-[1fr_220px_auto]" onSubmit={submit}>
        <label className="relative">
          <span className="sr-only">بحث</span>
          <Search className="absolute right-3.5 top-3.5 h-5 w-5 text-stone-400" />
          <Input className="pr-11" value={draftQuery} onChange={(event) => setDraftQuery(event.target.value)} placeholder="رقم الفاتورة أو اسم العميل أو الهاتف" />
        </label>
        <div className="hidden md:block"><InvoiceStatusFilter status={status} onChange={(next) => { setStatus(next); setPage(0); }} /></div>
        <details className="rounded-xl border border-stone-200 px-3 py-1 md:hidden">
          <summary className="flex min-h-10 list-none items-center gap-2 text-sm font-bold text-stone-700"><SlidersHorizontal className="h-4 w-4" />تصفية بالحالة</summary>
          <div className="border-t border-stone-100 py-3"><InvoiceStatusFilter status={status} onChange={(next) => { setStatus(next); setPage(0); }} /></div>
        </details>
        <Button type="submit" className="w-full md:w-auto" variant="secondary"><Search className="h-4 w-4" />بحث</Button>
      </form>
      {invoicesQuery.isPending ? <LoadingState label="جارٍ تحميل الفواتير..." /> : invoicesQuery.isError ? <ErrorState error={invoicesQuery.error} onRetry={() => void invoicesQuery.refetch()} /> : invoicesQuery.data.content.length === 0 ? <EmptyState title="لا توجد فواتير مطابقة" description="غيّر معايير البحث أو أضف أوردرًا من شاشة أوردر جديد." /> : <>
        <div className="space-y-3 md:hidden"><MobileInvoiceList invoices={invoicesQuery.data.content} /></div>
        <DataTableWrapper className="hidden md:block" label="قائمة الفواتير">
          <thead className={tableStyles.head}><tr><th>رقم الفاتورة</th><th>التاريخ</th><th>العميل</th><th>الإجمالي</th><th>الخصم</th><th>الحالة</th><th>الإجراء</th></tr></thead>
          <tbody className={tableStyles.body}>{invoicesQuery.data.content.map((invoice) => <tr key={invoice.id}><td className="font-mono font-black text-stone-950" dir="ltr">{invoice.invoiceNumber}</td><td>{formatArabicDate(invoice.businessDate)}</td><td><p className="font-bold">{invoice.customerName || "عميل نقدي"}</p>{invoice.customerPhone && <p className="mt-1 text-xs text-stone-400" dir="ltr">{invoice.customerPhone}</p>}</td><td className="font-black tabular-nums">{formatEgp(invoice.finalTotal)}</td><td className="tabular-nums">{formatEgp(invoice.discountTotal)}</td><td><StatusBadge tone={statusTones[invoice.status]}>{invoiceStatusLabels[invoice.status]}</StatusBadge></td><td><InvoiceAction invoice={invoice} /></td></tr>)}</tbody>
        </DataTableWrapper>
        <PaginationControls page={invoicesQuery.data.page} totalPages={invoicesQuery.data.totalPages} totalElements={invoicesQuery.data.totalElements} onPageChange={setPage} />
      </>}
    </div>
  );
}

function InvoiceStatusFilter({ status, onChange }: { status: InvoiceStatus | ""; onChange: (status: InvoiceStatus | "") => void }) {
  return <Select aria-label="حالة الفاتورة" value={status} onChange={(event) => onChange(event.target.value as InvoiceStatus | "")}><option value="">كل الحالات</option>{Object.entries(invoiceStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</Select>;
}

function MobileInvoiceList({ invoices }: { invoices: InvoiceSummary[] }) {
  return invoices.map((invoice) => (
    <article key={invoice.id} className="rounded-[1.5rem] border border-[#eee7ea] bg-white p-4 shadow-[0_10px_30px_rgb(41_37_42_/_0.04)]">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0"><p className="truncate font-mono text-sm font-black text-stone-950" dir="ltr">{invoice.invoiceNumber}</p><p className="mt-1 font-bold text-stone-800">{invoice.customerName || "عميل نقدي"}</p>{invoice.customerPhone && <p className="mt-1 text-xs text-stone-400" dir="ltr">{invoice.customerPhone}</p>}</div>
        <StatusBadge tone={statusTones[invoice.status]}>{invoiceStatusLabels[invoice.status]}</StatusBadge>
      </div>
      <div className="mt-4 flex items-end justify-between gap-3 border-t border-stone-100 pt-3"><div><p className="text-xs font-bold text-stone-400">{formatArabicDate(invoice.businessDate)}</p><p className="mt-1 text-lg font-black tabular-nums text-rose-800">{formatEgp(invoice.finalTotal)}</p></div><InvoiceAction invoice={invoice} /></div>
    </article>
  ));
}

function InvoiceAction({ invoice }: { invoice: InvoiceSummary }) {
  const isDraft = invoice.status === "DRAFT";
  return <Button asChild size="sm" variant={isDraft ? "default" : "outline"}><Link to={isDraft ? `/sales/invoices/${invoice.id}/edit` : `/sales/invoices/${invoice.id}`}>{isDraft ? <Pencil className="h-4 w-4" /> : <Eye className="h-4 w-4" />}{isDraft ? "فتح المسودة" : "التفاصيل"}</Link></Button>;
}
