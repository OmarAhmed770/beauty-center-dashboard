import { CalendarCheck, Eye, Pencil, Plus, Search, UserCheck, UserX } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
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
import { useAuth } from "@/features/auth/auth-context";
import { EmployeeFormDialog } from "@/features/employees/employee-form-dialog";
import { employeeKeys, searchEmployees, setEmployeeActive } from "@/features/employees/api";
import type { Employee } from "@/features/employees/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

export function EmployeesPage() {
  const { hasPermission, hasAnyRole } = useAuth();
  const canManage = hasPermission("EMPLOYEES");
  const isOwner = hasAnyRole(["OWNER"]);
  const queryClient = useQueryClient();
  const [draftQuery, setDraftQuery] = useState("");
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<"all" | "active" | "inactive">("all");
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null);
  const [statusTarget, setStatusTarget] = useState<Employee | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const params = { query: query || undefined, active: activeFilter === "all" ? undefined : activeFilter === "active", page, size: 20 };
  const employeesQuery = useQuery({ queryKey: employeeKeys.list(params), queryFn: () => searchEmployees(params) });
  const statusMutation = useMutation({
    mutationFn: (employee: Employee) => setEmployeeActive(employee.id, !employee.active),
    onSuccess: async () => {
      setStatusTarget(null);
      setActionError(null);
      await queryClient.invalidateQueries({ queryKey: employeeKeys.all });
    },
    onError: (error) => setActionError(getArabicApiErrorMessage(error)),
  });

  const submitSearch = (event: FormEvent) => {
    event.preventDefault();
    setPage(0);
    setQuery(draftQuery.trim());
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="الموظفون"
        description="اختر الموظف لتسجيل الأجر أو السلفة أو العمولة بسهولة."
        actions={canManage && <>{isOwner && <Button asChild variant="outline"><Link to="/employees/attendance"><CalendarCheck className="h-4 w-4" />تسجيل الحضور</Link></Button>}<Button onClick={() => { setEditingEmployee(null); setFormOpen(true); }}><Plus className="h-4 w-4" />إضافة موظف</Button></>}
      />

      <form className="grid gap-3 rounded-[1.5rem] border border-[#eee7ea] bg-white p-4 shadow-[0_10px_30px_rgb(41_37_42_/_0.04)] md:grid-cols-[1fr_220px_auto]" onSubmit={submitSearch}>
        <label className="relative"><span className="sr-only">بحث</span><Search className="absolute right-3.5 top-3.5 h-5 w-5 text-stone-400" /><Input className="pr-11" value={draftQuery} onChange={(event) => setDraftQuery(event.target.value)} placeholder="الاسم أو الهاتف أو المسمى الوظيفي" /></label>
        <Select aria-label="حالة الموظف" value={activeFilter} onChange={(event) => { setActiveFilter(event.target.value as typeof activeFilter); setPage(0); }}><option value="all">كل الحالات</option><option value="active">نشط</option><option value="inactive">غير نشط</option></Select>
        <Button type="submit" variant="secondary"><Search className="h-4 w-4" />بحث</Button>
      </form>

      {actionError && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-bold text-red-800" role="alert">{actionError}</div>}
      {employeesQuery.isPending ? <LoadingState label="جارٍ تحميل الموظفين..." /> : employeesQuery.isError ? <ErrorState error={employeesQuery.error} onRetry={() => void employeesQuery.refetch()} /> : employeesQuery.data.content.length === 0 ? <EmptyState title="لا يوجد موظفون مطابقون" description="غيّر معايير البحث أو أضف موظفًا جديدًا إذا كانت لديك الصلاحية." /> : (
        <>
          <DataTableWrapper label="قائمة الموظفين">
            <thead className={tableStyles.head}><tr><th>الموظف</th><th>المسمى</th><th>الحالة</th><th className="w-64">الإجراءات</th></tr></thead>
            <tbody className={tableStyles.body}>
              {employeesQuery.data.content.map((employee) => (
                <tr key={employee.id}>
                  <td><p className="font-black text-stone-900">{employee.name}</p><p className="mt-1 text-xs text-stone-400">رقم #{employee.id}</p></td>
                  <td>{employee.position}</td><td><StatusBadge tone={employee.active ? "success" : "neutral"}>{employee.active ? "نشط" : "غير نشط"}</StatusBadge></td>
                  <td><div className="flex flex-wrap gap-2"><Button asChild size="sm" variant="outline"><Link to={`/employees/${employee.id}`}><Eye className="h-4 w-4" />التفاصيل</Link></Button>{canManage && <><Button size="sm" variant="ghost" onClick={() => { setEditingEmployee(employee); setFormOpen(true); }}><Pencil className="h-4 w-4" />تعديل</Button><Button size="sm" variant="ghost" className={employee.active ? "text-red-700" : "text-emerald-700"} onClick={() => { setActionError(null); setStatusTarget(employee); }}>{employee.active ? <UserX className="h-4 w-4" /> : <UserCheck className="h-4 w-4" />}{employee.active ? "إيقاف" : "تفعيل"}</Button></>}</div></td>
                </tr>
              ))}
            </tbody>
          </DataTableWrapper>
          <PaginationControls page={employeesQuery.data.page} totalPages={employeesQuery.data.totalPages} totalElements={employeesQuery.data.totalElements} onPageChange={setPage} />
        </>
      )}

      <EmployeeFormDialog open={formOpen} employee={editingEmployee} onOpenChange={setFormOpen} />
      <ConfirmationDialog open={Boolean(statusTarget)} title={statusTarget?.active ? "إيقاف الموظف" : "تفعيل الموظف"} description={statusTarget?.active ? `سيظل سجل ${statusTarget.name} محفوظًا، لكن لن يمكن تسجيل حركات مالية جديدة له حتى إعادة التفعيل.` : `سيصبح ${statusTarget?.name ?? "الموظف"} متاحًا لتسجيل الحركات المالية الجديدة.`} confirmLabel={statusTarget?.active ? "إيقاف" : "تفعيل"} destructive={Boolean(statusTarget?.active)} busy={statusMutation.isPending} onOpenChange={(open) => !open && setStatusTarget(null)} onConfirm={() => statusTarget && statusMutation.mutate(statusTarget)} />
    </div>
  );
}
