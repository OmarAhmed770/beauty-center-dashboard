import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { expenseCategoryKeys, searchExpenseCategories } from "@/features/expense-categories/api";
import type { ExpenseStatus } from "@/features/expenses/types";
import { paymentMethodKeys, searchPaymentMethods } from "@/features/payment-methods/api";
import { getExpenseReport, reportKeys } from "@/features/reports/api";
import { ReportPageHeader } from "@/features/reports/report-page-header";
import { formatArabicDate, formatEgp } from "@/lib/format";

export function ExpenseReportPage() {
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("CURRENT_MONTH"));
  const [categoryId, setCategoryId] = useState(0);
  const [paymentMethodId, setPaymentMethodId] = useState(0);
  const [status, setStatus] = useState<ExpenseStatus | "">("ACTIVE");
  const [query, setQuery] = useState("");
  const categories = useQuery({ queryKey: expenseCategoryKeys.all, queryFn: () => searchExpenseCategories({ page: 0, size: 100 }) });
  const methods = useQuery({ queryKey: paymentMethodKeys.all, queryFn: () => searchPaymentMethods({ page: 0, size: 100 }) });
  const params = { fromDate: range.from, toDate: range.to, categoryId: categoryId || undefined, paymentMethodId: paymentMethodId || undefined, status: status || undefined, query: query.trim() || undefined, page: 0, size: 500 };
  const report = useQuery({ queryKey: reportKeys.detail("expenses", params), queryFn: () => getExpenseReport(params) });
  const total = report.data?.content.reduce((sum, row) => row.status === "ACTIVE" ? sum + Number(row.amount) : sum, 0) ?? 0;
  return <div className="space-y-6"><ReportPageHeader title="تقرير المصروفات" description="قائمة قابلة للطباعة مع التصنيف وطريقة الدفع والحالة وإجمالي النشط في النتائج." /><DateRangeFilter value={range} onChange={setRange} /><Card className="grid gap-3 p-4 md:grid-cols-4"><Select aria-label="تصنيف المصروف" value={categoryId || ""} onChange={(event) => setCategoryId(Number(event.target.value))}><option value="">كل التصنيفات</option>{categories.data?.content.map((row) => <option key={row.id} value={row.id}>{row.nameArabic}</option>)}</Select><Select aria-label="طريقة الدفع" value={paymentMethodId || ""} onChange={(event) => setPaymentMethodId(Number(event.target.value))}><option value="">كل طرق الدفع</option>{methods.data?.content.map((row) => <option key={row.id} value={row.id}>{row.displayNameArabic}</option>)}</Select><Select aria-label="حالة المصروف" value={status} onChange={(event) => setStatus(event.target.value as ExpenseStatus | "")}><option value="">كل الحالات</option><option value="ACTIVE">نشط</option><option value="CANCELLED">ملغي</option></Select><Input aria-label="بحث المصروفات" placeholder="الوصف أو المرجع" value={query} onChange={(event) => setQuery(event.target.value)} /></Card>{report.isPending ? <LoadingState /> : report.isError ? <ErrorState error={report.error} onRetry={() => void report.refetch()} /> : report.data.content.length === 0 ? <EmptyState /> : <PrintableViewWrapper title="طباعة تقرير المصروفات"><article className="space-y-5" dir="rtl"><header className="border-b border-stone-200 pb-4"><h2 className="text-2xl font-black">تقرير المصروفات</h2><p className="mt-2 text-sm text-stone-500">{formatArabicDate(range.from)} إلى {formatArabicDate(range.to)}</p><p className="mt-2 font-black">إجمالي المصروفات النشطة في النتائج: {formatEgp(total)}</p></header><DataTableWrapper label="المصروفات"><thead className={tableStyles.head}><tr><th>التاريخ</th><th>التصنيف</th><th>الوصف</th><th>طريقة الدفع</th><th>المبلغ</th><th>الحالة</th></tr></thead><tbody className={tableStyles.body}>{report.data.content.map((row) => <tr key={row.id}><td>{formatArabicDate(row.businessDate)}</td><td>{row.categoryNameArabic}</td><td>{row.description}</td><td>{row.paymentMethodNameArabic}</td><td className="font-black">{formatEgp(row.amount)}</td><td>{row.status === "ACTIVE" ? "نشط" : "ملغي"}</td></tr>)}</tbody></DataTableWrapper>{report.data.totalElements > report.data.content.length && <p className="text-xs font-bold text-amber-800">يعرض التقرير أول 500 سجل؛ ضيّق الفترة لعرض البقية.</p>}</article></PrintableViewWrapper>}</div>;
}
