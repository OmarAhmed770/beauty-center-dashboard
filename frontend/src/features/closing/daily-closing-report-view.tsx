import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { StatusBadge } from "@/components/shared/status-badge";
import type { DailyClosing } from "@/features/closing/types";
import { closingStatusLabels } from "@/features/closing/model";
import { formatArabicDate, formatArabicDateTime, formatEgp, formatNumber } from "@/lib/format";

const statusTone = { OPEN: "warning", CLOSED: "success", REOPENED: "info" } as const;

export function DailyClosingReportView({ closing }: { closing: DailyClosing }) {
  const totals = [
    ["إجمالي المبيعات", closing.grossSales], ["الخصومات", closing.discounts], ["المستردات", closing.refunds],
    ["صافي المبيعات", closing.netSales], ["المصروفات", closing.expenseTotal], ["تكلفة الموظفين", closing.employeeCost],
    ["تكلفة المباع", closing.costOfSoldItemsAndHair], ["صافي الربح", closing.netProfit], ["النقدية المتوقعة", closing.expectedCash],
    ["النقدية الفعلية", closing.actualCash], ["فرق النقدية", closing.cashDifference],
  ] as const;
  return (
    <article className="space-y-6" dir="rtl">
      <header className="flex flex-wrap items-start justify-between gap-4 border-b border-stone-200 pb-4">
        <div><p className="text-xs font-black text-rose-700">مركز التجميل</p><h2 className="mt-2 text-2xl font-black">ملخص الإغلاق اليومي</h2><p className="mt-2 text-sm text-stone-500">تاريخ العمل: {formatArabicDate(closing.businessDate)}</p></div>
        <div className="text-left"><StatusBadge tone={statusTone[closing.status]}>{closingStatusLabels[closing.status]}</StatusBadge><p className="mt-2 text-xs text-stone-500">النسخة {formatNumber(closing.closingVersion)} · أُغلق: {formatArabicDateTime(closing.closedAt)}</p></div>
      </header>
      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">{totals.map(([label, value]) => <div key={label} className="rounded-xl bg-stone-50 p-4"><p className="text-xs font-bold text-stone-500">{label}</p><p className="mt-2 text-lg font-black tabular-nums">{formatEgp(value)}</p></div>)}</section>
      <section><h3 className="mb-3 text-lg font-black">تفصيل طرق الدفع</h3>{closing.paymentMethodBreakdown.length === 0 ? <p className="text-sm text-stone-500">لا توجد دفعات.</p> : <DataTableWrapper label="تفصيل طرق الدفع"><thead className={tableStyles.head}><tr><th>الطريقة</th><th>المحصل</th><th>المسترد</th><th>الصافي</th></tr></thead><tbody className={tableStyles.body}>{closing.paymentMethodBreakdown.map((row) => <tr key={`${row.paymentMethodId}-${row.code}`}><td>{row.displayNameArabic}</td><td>{formatEgp(row.receivedAmount)}</td><td>{formatEgp(row.refundedAmount)}</td><td className="font-black">{formatEgp(row.netAmount)}</td></tr>)}</tbody></DataTableWrapper>}</section>
      <section><h3 className="mb-3 text-lg font-black">تفصيل المصروفات</h3>{closing.expenseCategoryBreakdown.length === 0 ? <p className="text-sm text-stone-500">لا توجد مصروفات.</p> : <DataTableWrapper label="تفصيل المصروفات"><thead className={tableStyles.head}><tr><th>التصنيف</th><th>العدد</th><th>الإجمالي</th></tr></thead><tbody className={tableStyles.body}>{closing.expenseCategoryBreakdown.map((row) => <tr key={`${row.categoryId}-${row.nameArabic}`}><td>{row.nameArabic}</td><td>{formatNumber(row.expenseCount)}</td><td className="font-black">{formatEgp(row.amount)}</td></tr>)}</tbody></DataTableWrapper>}</section>
      <section className="grid gap-3 rounded-xl border border-stone-200 p-4 sm:grid-cols-2"><div><p className="text-xs font-bold text-stone-500">أغلق بواسطة</p><p className="mt-1 font-black">{closing.closedBy}</p></div><div><p className="text-xs font-bold text-stone-500">عدد مرات إعادة الفتح</p><p className="mt-1 font-black">{formatNumber(closing.reopenCount)}</p></div>{closing.notes && <div className="sm:col-span-2"><p className="text-xs font-bold text-stone-500">ملاحظات الإغلاق</p><p className="mt-1 whitespace-pre-wrap text-sm">{closing.notes}</p></div>}{closing.reopeningReason && <div className="sm:col-span-2"><p className="text-xs font-bold text-stone-500">سبب آخر إعادة فتح</p><p className="mt-1 text-sm">{closing.reopeningReason} — {closing.reopenedBy}</p></div>}</section>
    </article>
  );
}
