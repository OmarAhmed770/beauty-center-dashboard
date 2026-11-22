import { FileText, Plus, Search, SlidersHorizontal, XCircle } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type FormEvent } from "react";
import { useSearchParams } from "react-router-dom";
import { cairoDateIso, DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { PaginationControls } from "@/components/shared/pagination-controls";
import { ReasonDialog } from "@/components/shared/reason-dialog";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { useAuth } from "@/features/auth/auth-context";
import { expenseCategoryKeys, searchExpenseCategories } from "@/features/expense-categories/api";
import { cancelExpense, expenseKeys, getExpenseSummary, getExpenseTotalsByCategory, getExpenseTotalsByPaymentMethod, searchExpenses } from "@/features/expenses/api";
import { ExpenseFormDialog } from "@/features/expenses/expense-form-dialog";
import type { Expense, ExpenseStatus } from "@/features/expenses/types";
import { paymentMethodKeys, searchPaymentMethods } from "@/features/payment-methods/api";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { invalidateFinancialViews } from "@/lib/financial-query-invalidation";
import { formatArabicDate, formatArabicDateTime, formatEgp } from "@/lib/format";

export function ExpensesPage() {
  const [searchParams] = useSearchParams();
  const { hasPermission } = useAuth();
  const canManageSettings = hasPermission("SETTINGS");
  const canCancelOld = canManageSettings;
  const queryClient = useQueryClient();
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("TODAY"));
  const [draftQuery, setDraftQuery] = useState("");
  const [query, setQuery] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [paymentMethodId, setPaymentMethodId] = useState("");
  const [status, setStatus] = useState<ExpenseStatus | "">("");
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(() => searchParams.get("new") === "1");
  const [cancelTarget, setCancelTarget] = useState<Expense | null>(null);
  const [cancelError, setCancelError] = useState<string | null>(null);

  useEffect(() => {
    if (searchParams.get("new") === "1") setFormOpen(true);
  }, [searchParams]);

  const filters = {
    categoryId: categoryId ? Number(categoryId) : undefined,
    paymentMethodId: paymentMethodId ? Number(paymentMethodId) : undefined,
    status: status || undefined,
    fromDate: range.from,
    toDate: range.to,
    query: query || undefined,
    page,
    size: 20,
  };
  const expensesQuery = useQuery({ queryKey: expenseKeys.list(filters), queryFn: () => searchExpenses(filters) });
  const summaryParams = { fromDate: range.from, toDate: range.to };
  const summaryQuery = useQuery({ queryKey: expenseKeys.summary(summaryParams), queryFn: () => getExpenseSummary(summaryParams) });
  const categoryTotalsQuery = useQuery({ queryKey: expenseKeys.byCategory(summaryParams), queryFn: () => getExpenseTotalsByCategory(summaryParams) });
  const paymentTotalsQuery = useQuery({ queryKey: expenseKeys.byPayment(summaryParams), queryFn: () => getExpenseTotalsByPaymentMethod(summaryParams) });
  const categoriesQuery = useQuery({ queryKey: [...expenseCategoryKeys.all, "filters"], queryFn: () => searchExpenseCategories({ page: 0, size: 100 }), staleTime: 5 * 60_000 });
  const paymentMethodsQuery = useQuery({ queryKey: [...paymentMethodKeys.all, "filters"], queryFn: () => searchPaymentMethods({ page: 0, size: 100 }), staleTime: 5 * 60_000 });
  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => cancelExpense(id, reason),
    onSuccess: async () => { setCancelTarget(null); setCancelError(null); await Promise.all([queryClient.invalidateQueries({ queryKey: expenseKeys.all }), invalidateFinancialViews(queryClient)]); },
    onError: (error) => setCancelError(getArabicApiErrorMessage(error)),
  });

  const updateRange = (nextRange: DateRangeValue) => { setRange(nextRange); setPage(0); };
  const submitSearch = (event: FormEvent) => { event.preventDefault(); setQuery(draftQuery.trim()); setPage(0); };
  const canCancel = (expense: Expense) => expense.status === "ACTIVE" && (canCancelOld || expense.businessDate >= cairoDateIso());

  return (
    <div className="space-y-7">
      <PageHeader title="مصروفات اليوم" description="سجّل مصروف اليوم سريعًا، ثم راجع التفاصيل عند الحاجة." actions={<Button size="lg" onClick={() => setFormOpen(true)}><Plus className="h-5 w-5" />إضافة مصروف</Button>} />

      <details className="rounded-2xl border border-[#eee7ea] bg-white px-4 shadow-sm">
        <summary className="flex min-h-13 cursor-pointer list-none items-center text-sm font-black text-[#736873]">تفاصيل الفترة والملخص</summary>
        <div className="space-y-5 border-t border-[#f1ebee] py-4"><DateRangeFilter value={range} onChange={updateRange} />
          {summaryQuery.isPending ? <LoadingState label="جارٍ احتساب ملخص المصروفات..." /> : summaryQuery.isError ? <ErrorState error={summaryQuery.error} onRetry={() => void summaryQuery.refetch()} /> : <section className="grid grid-cols-2 gap-3 xl:grid-cols-5" aria-label="ملخص المصروفات"><SummaryCard label="إجمالي المصروفات" value={formatEgp(summaryQuery.data.totalAmount)} /><SummaryCard label="مصروفات نقدية" value={formatEgp(summaryQuery.data.cashAmount)} /><SummaryCard label="مصروفات غير نقدية" value={formatEgp(summaryQuery.data.nonCashAmount)} /><SummaryCard label="مصروفات نشطة" value={`${summaryQuery.data.activeExpenseCount} سجل`} /><SummaryCard label="مصروفات ملغاة" value={`${summaryQuery.data.cancelledExpenseCount} سجل`} tone="text-red-700" /></section>}
        </div>
      </details>

      <details className="rounded-2xl border border-[#eee7ea] bg-white px-4 shadow-sm">
        <summary className="flex min-h-13 cursor-pointer list-none items-center gap-2 text-sm font-black text-[#736873]"><SlidersHorizontal className="h-4 w-4" />بحث وتصفية</summary>
        <form className="grid gap-3 border-t border-[#f1ebee] py-4 lg:grid-cols-[1fr_180px_180px_150px_auto]" onSubmit={submitSearch}>
        <label className="relative"><span className="sr-only">بحث في المصروفات</span><Search className="absolute right-3.5 top-3.5 h-5 w-5 text-stone-400" /><Input className="pr-11" value={draftQuery} onChange={(event) => setDraftQuery(event.target.value)} placeholder="الوصف أو المرجع" /></label>
        <div className="hidden lg:contents"><Select aria-label="تصنيف المصروف" value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setPage(0); }}><option value="">كل التصنيفات</option>{categoriesQuery.data?.content.map((category) => <option key={category.id} value={category.id}>{category.nameArabic}</option>)}</Select><Select aria-label="طريقة الدفع" value={paymentMethodId} onChange={(event) => { setPaymentMethodId(event.target.value); setPage(0); }}><option value="">كل طرق الدفع</option>{paymentMethodsQuery.data?.content.map((method) => <option key={method.id} value={method.id}>{method.displayNameArabic}</option>)}</Select><Select aria-label="حالة المصروف" value={status} onChange={(event) => { setStatus(event.target.value as ExpenseStatus | ""); setPage(0); }}><option value="">كل الحالات</option><option value="ACTIVE">نشط</option><option value="CANCELLED">ملغى</option></Select></div>
        <details className="rounded-xl border border-stone-200 px-3 py-1 lg:hidden"><summary className="flex min-h-10 list-none items-center gap-2 text-sm font-bold text-stone-700"><SlidersHorizontal className="h-4 w-4" />تصفية القائمة</summary><div className="mt-1 grid gap-3 border-t border-stone-100 py-3"><Select aria-label="تصنيف المصروف" value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setPage(0); }}><option value="">كل التصنيفات</option>{categoriesQuery.data?.content.map((category) => <option key={category.id} value={category.id}>{category.nameArabic}</option>)}</Select><Select aria-label="طريقة الدفع" value={paymentMethodId} onChange={(event) => { setPaymentMethodId(event.target.value); setPage(0); }}><option value="">كل طرق الدفع</option>{paymentMethodsQuery.data?.content.map((method) => <option key={method.id} value={method.id}>{method.displayNameArabic}</option>)}</Select><Select aria-label="حالة المصروف" value={status} onChange={(event) => { setStatus(event.target.value as ExpenseStatus | ""); setPage(0); }}><option value="">كل الحالات</option><option value="ACTIVE">نشط</option><option value="CANCELLED">ملغى</option></Select></div></details>
        <Button type="submit" className="w-full lg:w-auto" variant="secondary"><Search className="h-4 w-4" />بحث</Button>
        </form>
      </details>

      {expensesQuery.isPending ? <LoadingState label="جارٍ تحميل المصروفات..." /> : expensesQuery.isError ? <ErrorState error={expensesQuery.error} onRetry={() => void expensesQuery.refetch()} /> : expensesQuery.data.content.length === 0 ? <EmptyState title="لا توجد مصروفات مطابقة" description="غيّر الفترة أو معايير التصفية، أو سجل مصروفًا جديدًا." /> : <>
        <div className="space-y-3 lg:hidden"><MobileExpenseList expenses={expensesQuery.data.content} canCancel={canCancel} onCancel={(expense) => { setCancelError(null); setCancelTarget(expense); }} /></div>
        <DataTableWrapper className="hidden lg:block" label="قائمة المصروفات"><thead className={tableStyles.head}><tr><th>التاريخ</th><th>التصنيف والوصف</th><th>المبلغ</th><th>طريقة الدفع</th><th>الحالة</th><th>الإيصال</th><th>الإجراء</th></tr></thead><tbody className={tableStyles.body}>{expensesQuery.data.content.map((expense) => <tr key={expense.id} className={expense.status === "CANCELLED" ? "opacity-60" : undefined}><td>{formatArabicDate(expense.businessDate)}</td><td><p className="font-black text-stone-900">{expense.categoryNameArabic}</p><p className="mt-1 max-w-72 text-xs text-stone-500">{expense.description}</p><p className="mt-1 text-xs text-stone-400">أُنشئ {formatArabicDateTime(expense.createdAt)}</p></td><td className="font-black tabular-nums">{formatEgp(expense.amount)}</td><td><p className="font-bold">{expense.paymentMethodNameArabic}</p>{expense.paymentReference && <p className="mt-1 text-xs text-stone-400" dir="ltr">{expense.paymentReference}</p>}</td><td><StatusBadge tone={expense.status === "ACTIVE" ? "success" : "danger"}>{expense.status === "ACTIVE" ? "نشط" : "ملغى"}</StatusBadge>{expense.cancellationReason && <p className="mt-1 max-w-48 text-xs text-red-700">{expense.cancellationReason}</p>}</td><td>{expense.receiptPath ? <span className="inline-flex items-center gap-1 text-xs font-bold text-stone-600" title={expense.receiptPath}><FileText className="h-4 w-4" />مسار مسجل</span> : "—"}</td><td>{canCancel(expense) && <Button size="sm" variant="ghost" className="text-red-700" onClick={() => { setCancelError(null); setCancelTarget(expense); }}><XCircle className="h-4 w-4" />إلغاء</Button>}</td></tr>)}</tbody></DataTableWrapper>
        <PaginationControls page={expensesQuery.data.page} totalPages={expensesQuery.data.totalPages} totalElements={expensesQuery.data.totalElements} onPageChange={setPage} />
      </>}

      <details className="rounded-2xl border border-[#eee7ea] bg-white px-4 shadow-sm"><summary className="flex min-h-13 cursor-pointer list-none items-center text-sm font-black text-[#736873]">توزيع المصروفات</summary><section className="grid gap-4 border-t border-[#f1ebee] py-4 xl:grid-cols-2">
        <BreakdownCard title="التوزيع حسب التصنيف" loading={categoryTotalsQuery.isPending} error={categoryTotalsQuery.isError ? categoryTotalsQuery.error : undefined} rows={categoryTotalsQuery.data?.map((item) => ({ id: item.categoryId, label: item.nameArabic, amount: item.totalAmount, count: item.expenseCount }))} />
        <BreakdownCard title="التوزيع حسب طريقة الدفع" loading={paymentTotalsQuery.isPending} error={paymentTotalsQuery.isError ? paymentTotalsQuery.error : undefined} rows={paymentTotalsQuery.data?.map((item) => ({ id: item.paymentMethodId, label: item.displayNameArabic, amount: item.totalAmount, count: item.expenseCount }))} />
      </section></details>

      <ExpenseFormDialog open={formOpen} onOpenChange={setFormOpen} />
      <ReasonDialog open={Boolean(cancelTarget)} title="إلغاء المصروف" description="لن يُحذف المصروف؛ سيُحفظ سبب الإلغاء ويُستبعد من الإجماليات النشطة." busy={cancelMutation.isPending} error={cancelError} onOpenChange={(open) => { if (!open) { setCancelTarget(null); setCancelError(null); } }} onConfirm={(reason) => cancelTarget && cancelMutation.mutate({ id: cancelTarget.id, reason })} />
    </div>
  );
}

function SummaryCard({ label, value, tone = "text-stone-950" }: { label: string; value: string; tone?: string }) {
  return <Card className="p-4"><p className="text-xs font-bold text-stone-500">{label}</p><p className={`mt-3 text-xl font-black tabular-nums ${tone}`}>{value}</p></Card>;
}

function MobileExpenseList({ expenses, canCancel, onCancel }: { expenses: Expense[]; canCancel: (expense: Expense) => boolean; onCancel: (expense: Expense) => void }) {
  return expenses.map((expense) => (
    <article key={expense.id} className={`rounded-[1.5rem] border border-[#eee7ea] bg-white p-4 shadow-[0_10px_30px_rgb(41_37_42_/_0.04)] ${expense.status === "CANCELLED" ? "opacity-60" : ""}`}>
      <div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="line-clamp-2 font-black text-stone-900">{expense.description}</p><p className="mt-1 text-xs font-bold text-stone-500">{expense.categoryNameArabic}</p></div><StatusBadge tone={expense.status === "ACTIVE" ? "success" : "danger"}>{expense.status === "ACTIVE" ? "نشط" : "ملغى"}</StatusBadge></div>
      <div className="mt-4 grid grid-cols-2 gap-3 border-t border-stone-100 pt-3 text-sm"><div><p className="text-xs font-bold text-stone-400">طريقة الدفع</p><p className="mt-1 font-bold text-stone-800">{expense.paymentMethodNameArabic}</p></div><div><p className="text-xs font-bold text-stone-400">التاريخ</p><p className="mt-1 font-bold text-stone-800">{formatArabicDate(expense.businessDate)}</p></div></div>
      <div className="mt-4 flex items-end justify-between gap-3"><div><p className="text-xs font-bold text-stone-400">المبلغ</p><p className="mt-1 text-lg font-black tabular-nums text-rose-800">{formatEgp(expense.amount)}</p></div>{canCancel(expense) && <Button size="sm" variant="ghost" className="text-red-700" onClick={() => onCancel(expense)}><XCircle className="h-4 w-4" />إلغاء</Button>}</div>
      {expense.cancellationReason && <p className="mt-3 border-t border-red-100 pt-3 text-xs font-bold text-red-700">سبب الإلغاء: {expense.cancellationReason}</p>}
    </article>
  ));
}

function BreakdownCard({ title, rows, loading, error }: { title: string; rows?: Array<{ id: number; label: string; amount: number | string; count: number }>; loading: boolean; error?: unknown }) {
  return <Card className="p-5"><h2 className="font-black text-stone-950">{title}</h2>{loading ? <p className="mt-4 text-sm text-stone-500">جارٍ التحميل...</p> : error ? <p className="mt-4 text-sm font-bold text-red-700">{getArabicApiErrorMessage(error)}</p> : !rows?.length ? <p className="mt-4 text-sm text-stone-500">لا توجد بيانات في هذه الفترة.</p> : <ul className="mt-4 divide-y divide-stone-100">{rows.map((row) => <li key={row.id} className="flex items-center justify-between gap-4 py-3"><span><strong className="block text-sm text-stone-800">{row.label}</strong><small className="text-stone-400">{row.count} سجل</small></span><strong className="tabular-nums text-stone-950">{formatEgp(row.amount)}</strong></li>)}</ul>}</Card>;
}
