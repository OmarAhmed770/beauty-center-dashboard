import { ArrowRight, Banknote, CalendarDays, Pencil, Phone, Plus, UserRound, XCircle } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
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
import { useAuth } from "@/features/auth/auth-context";
import { cancelEmployeeTransaction, employeeKeys, getEmployee, getEmployeeStatement } from "@/features/employees/api";
import { EmployeeFormDialog } from "@/features/employees/employee-form-dialog";
import { employeeTransactionTypeLabels } from "@/features/employees/model";
import { TransactionFormDialog } from "@/features/employees/transaction-form-dialog";
import type { EmployeeTransaction, EmployeeTotals, EmployeeTransactionType } from "@/features/employees/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { invalidateFinancialViews } from "@/lib/financial-query-invalidation";
import { formatArabicDate, formatArabicDateTime, formatEgp } from "@/lib/format";

const totalCards: Array<{ key: keyof EmployeeTotals; label: string; color: string }> = [
  { key: "dailyWages", label: "الأجور اليومية", color: "text-emerald-700" },
  { key: "bonuses", label: "المكافآت", color: "text-emerald-700" },
  { key: "commissions", label: "العمولات", color: "text-emerald-700" },
  { key: "deductions", label: "الخصومات", color: "text-red-700" },
  { key: "advances", label: "السلف", color: "text-amber-700" },
  { key: "payments", label: "المدفوعات", color: "text-sky-700" },
  { key: "netEntitlement", label: "صافي الاستحقاق", color: "text-rose-800" },
];

export function EmployeeDetailsPage() {
  const employeeId = Number(useParams<{ employeeId: string }>().employeeId);
  const { hasPermission } = useAuth();
  const canManage = hasPermission("EMPLOYEES");
  const queryClient = useQueryClient();
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("CURRENT_MONTH"));
  const [statementPage, setStatementPage] = useState(0);
  const [editOpen, setEditOpen] = useState(false);
  const [transactionOpen, setTransactionOpen] = useState(false);
  const [transactionType, setTransactionType] = useState<EmployeeTransactionType | undefined>();
  const [cancelTarget, setCancelTarget] = useState<EmployeeTransaction | null>(null);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const employeeQuery = useQuery({ queryKey: employeeKeys.detail(employeeId), queryFn: () => getEmployee(employeeId), enabled: Number.isInteger(employeeId) && employeeId > 0 });
  const statementParams = { fromDate: range.from, toDate: range.to, page: statementPage, size: 20 };
  const statementQuery = useQuery({ queryKey: employeeKeys.statement(employeeId, statementParams), queryFn: () => getEmployeeStatement(employeeId, statementParams), enabled: canManage && employeeQuery.isSuccess });
  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => cancelEmployeeTransaction(id, reason),
    onSuccess: async () => {
      setCancelTarget(null);
      setCancelError(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: employeeKeys.all }),
        invalidateFinancialViews(queryClient),
      ]);
    },
    onError: (error) => setCancelError(getArabicApiErrorMessage(error)),
  });

  if (employeeQuery.isPending) return <LoadingState label="جارٍ تحميل ملف الموظف..." />;
  if (employeeQuery.isError) return <ErrorState error={employeeQuery.error} onRetry={() => void employeeQuery.refetch()} />;
  const employee = employeeQuery.data;

  const updateRange = (nextRange: DateRangeValue) => {
    setRange(nextRange);
    setStatementPage(0);
  };
  const openTransaction = (type?: EmployeeTransactionType) => {
    setTransactionType(type);
    setTransactionOpen(true);
  };

  return (
    <div className="space-y-7">
      <PageHeader
        title={employee.name}
        description={`ملف الموظف #${employee.id} — ${employee.position}`}
        actions={<><Button asChild variant="outline"><Link to="/employees"><ArrowRight className="h-4 w-4" />قائمة الموظفين</Link></Button>{canManage && <Button variant="secondary" onClick={() => setEditOpen(true)}><Pencil className="h-4 w-4" />تعديل البيانات</Button>}<Button onClick={() => openTransaction()} disabled={!employee.active}><Plus className="h-4 w-4" />حركة أخرى</Button></>}
      />

      <section className="grid gap-4 lg:grid-cols-[1fr_1.4fr]">
        <Card className="p-6">
          <div className="flex items-center justify-between"><span className="grid h-12 w-12 place-items-center rounded-2xl bg-rose-100 text-rose-800"><UserRound /></span><StatusBadge tone={employee.active ? "success" : "neutral"}>{employee.active ? "نشط" : "غير نشط"}</StatusBadge></div>
          <dl className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
            <div><dt className="text-xs font-bold text-stone-400">المسمى الوظيفي</dt><dd className="mt-1 font-black text-stone-900">{employee.position}</dd></div>
            <div><dt className="text-xs font-bold text-stone-400">الهاتف</dt><dd className="mt-1 flex items-center gap-2 font-bold text-stone-700"><Phone className="h-4 w-4" />{employee.phone || "غير مسجل"}</dd></div>
            <div><dt className="text-xs font-bold text-stone-400">الأجر اليومي</dt><dd className="mt-1 flex items-center gap-2 font-black text-stone-900"><Banknote className="h-4 w-4" />{formatEgp(employee.defaultDailyWage)}</dd></div>
            <div><dt className="text-xs font-bold text-stone-400">آخر تحديث</dt><dd className="mt-1 text-sm font-bold text-stone-700">{formatArabicDateTime(employee.updatedAt)}</dd></div>
          </dl>
          {employee.notes && <div className="mt-6 rounded-xl bg-stone-50 p-4"><p className="text-xs font-bold text-stone-400">ملاحظات</p><p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-stone-700">{employee.notes}</p></div>}
        </Card>
        <Card className="p-6">
          <div className="flex items-center gap-3"><CalendarDays className="text-rose-700" /><div><h2 className="font-black text-stone-950">فترة كشف الحساب</h2><p className="mt-1 text-xs text-stone-500">الإجماليات تستبعد الحركات الملغاة.</p></div></div>
          {canManage ? <div className="mt-5"><DateRangeFilter value={range} onChange={updateRange} /></div> : <div className="mt-6 rounded-xl border border-sky-200 bg-sky-50 p-4 text-sm leading-7 text-sky-900">يمكنك عرض بيانات الموظف وتسجيل الأجر اليومي. كشف الحساب والإلغاءات متاحة للمالك والمدير فقط.</div>}
        </Card>
      </section>

      {canManage && <section className="rounded-2xl border border-[#eee7ea] bg-white p-4 shadow-sm"><h2 className="font-black text-stone-950">تسجيل سريع</h2><p className="mt-1 text-sm text-stone-500">سجّل الأجر من صفحة الحضور، ثم استخدم هذه الأزرار للسلف والعمولات والدفعات الفعلية.</p><div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4"><Button variant="outline" size="lg" onClick={() => openTransaction("DAILY_WAGE")} disabled={!employee.active}>تسجيل أجر يدوي</Button><Button variant="outline" size="lg" onClick={() => openTransaction("ADVANCE")} disabled={!employee.active}>تسجيل سلفة</Button><Button variant="outline" size="lg" onClick={() => openTransaction("COMMISSION")} disabled={!employee.active}>تسجيل عمولة</Button><Button variant="outline" size="lg" onClick={() => openTransaction("PAYMENT")} disabled={!employee.active}>دفع مستحقات</Button></div></section>}

      {canManage && (statementQuery.isPending ? <LoadingState label="جارٍ احتساب كشف الحساب..." /> : statementQuery.isError ? <ErrorState error={statementQuery.error} onRetry={() => void statementQuery.refetch()} /> : <>
        <section><h2 className="mb-4 text-lg font-black text-stone-950">الإجماليات</h2><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7">{totalCards.map((card) => <Card key={card.key} className="p-4"><p className="text-xs font-bold text-stone-500">{card.label}</p><p className={`mt-3 text-lg font-black tabular-nums ${card.color}`}>{formatEgp(statementQuery.data.totals[card.key] as number | string)}</p></Card>)}</div></section>
        <section className="space-y-4">
          <div><h2 className="text-lg font-black text-stone-950">سجل الحركات المالية</h2><p className="mt-1 text-sm text-stone-500">من {formatArabicDate(range.from)} إلى {formatArabicDate(range.to)}</p></div>
          {statementQuery.data.transactions.content.length === 0 ? <EmptyState title="لا توجد حركات في هذه الفترة" /> : <>
            <DataTableWrapper label="حركات الموظف المالية"><thead className={tableStyles.head}><tr><th>التاريخ</th><th>نوع الحركة</th><th>المبلغ</th><th>طريقة الدفع</th><th>الحالة</th><th>سجل الإنشاء</th><th>الإجراء</th></tr></thead><tbody className={tableStyles.body}>{statementQuery.data.transactions.content.map((transaction) => <tr key={transaction.id} className={transaction.status === "CANCELLED" ? "opacity-60" : undefined}><td>{formatArabicDate(transaction.businessDate)}</td><td><p className="font-bold text-stone-900">{employeeTransactionTypeLabels[transaction.type]}</p>{transaction.notes && <p className="mt-1 max-w-52 truncate text-xs text-stone-400" title={transaction.notes}>{transaction.notes}</p>}</td><td className="font-black tabular-nums">{formatEgp(transaction.amount)}</td><td>{transaction.paymentMethod || "—"}</td><td><StatusBadge tone={transaction.status === "ACTIVE" ? "success" : "danger"}>{transaction.status === "ACTIVE" ? "نشطة" : "ملغاة"}</StatusBadge>{transaction.cancellationReason && <p className="mt-1 max-w-48 text-xs text-red-700" title={transaction.cancellationReason}>{transaction.cancellationReason}</p>}</td><td><p className="text-xs font-bold">{transaction.createdBy || "النظام"}</p><p className="mt-1 text-xs text-stone-400">{formatArabicDateTime(transaction.createdAt)}</p></td><td>{transaction.status === "ACTIVE" && <Button size="sm" variant="ghost" className="text-red-700" onClick={() => { setCancelError(null); setCancelTarget(transaction); }}><XCircle className="h-4 w-4" />إلغاء</Button>}</td></tr>)}</tbody></DataTableWrapper>
            <PaginationControls page={statementQuery.data.transactions.page} totalPages={statementQuery.data.transactions.totalPages} totalElements={statementQuery.data.transactions.totalElements} onPageChange={setStatementPage} />
          </>}
        </section>
      </>)}

      <EmployeeFormDialog open={editOpen} employee={employee} onOpenChange={setEditOpen} />
      <TransactionFormDialog open={transactionOpen} employee={employee} initialType={transactionType} onOpenChange={setTransactionOpen} />
      <ReasonDialog open={Boolean(cancelTarget)} title="إلغاء الحركة المالية" description={`سيُحفظ الإلغاء في سجل التدقيق ولن تُحذف الحركة أو قيمتها التاريخية.`} busy={cancelMutation.isPending} error={cancelError} onOpenChange={(open) => { if (!open) { setCancelTarget(null); setCancelError(null); } }} onConfirm={(reason) => cancelTarget && cancelMutation.mutate({ id: cancelTarget.id, reason })} />
    </div>
  );
}
