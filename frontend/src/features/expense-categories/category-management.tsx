import { Pencil, Plus, Tags } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { CategoryFormDialog } from "@/features/expense-categories/category-form-dialog";
import { expenseCategoryKeys, searchExpenseCategories, setExpenseCategoryActive } from "@/features/expense-categories/api";
import type { ExpenseCategory } from "@/features/expense-categories/types";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

export function CategoryManagement() {
  const queryClient = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ExpenseCategory | null>(null);
  const [statusTarget, setStatusTarget] = useState<ExpenseCategory | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const query = useQuery({ queryKey: [...expenseCategoryKeys.all, "management"], queryFn: () => searchExpenseCategories({ page: 0, size: 100 }) });
  const statusMutation = useMutation({
    mutationFn: (category: ExpenseCategory) => setExpenseCategoryActive(category.id, !category.active),
    onSuccess: async () => { setStatusTarget(null); setActionError(null); await queryClient.invalidateQueries({ queryKey: expenseCategoryKeys.all }); },
    onError: (error) => setActionError(getArabicApiErrorMessage(error)),
  });

  return (
    <section className="space-y-4" aria-labelledby="category-management-heading">
      <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 id="category-management-heading" className="flex items-center gap-2 text-lg font-black text-stone-950"><Tags className="text-rose-700" />تصنيفات المصروفات</h2><p className="mt-1 text-sm text-stone-500">تنظيم المصروفات للتصفية والتقارير المالية.</p></div><Button onClick={() => { setEditing(null); setFormOpen(true); }}><Plus className="h-4 w-4" />إضافة تصنيف</Button></div>
      {actionError && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-bold text-red-800" role="alert">{actionError}</div>}
      {query.isPending ? <LoadingState label="جارٍ تحميل التصنيفات..." /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : query.data.content.length === 0 ? <EmptyState title="لا توجد تصنيفات" /> : <DataTableWrapper label="تصنيفات المصروفات"><thead className={tableStyles.head}><tr><th>الاسم العربي</th><th>الاسم الإنجليزي</th><th>الحالة</th><th>الإجراءات</th></tr></thead><tbody className={tableStyles.body}>{query.data.content.map((category) => <tr key={category.id}><td className="font-black text-stone-900">{category.nameArabic}</td><td dir="ltr" className="text-right">{category.nameEnglish}</td><td><StatusBadge tone={category.active ? "success" : "neutral"}>{category.active ? "نشط" : "غير نشط"}</StatusBadge></td><td><div className="flex gap-2"><Button size="sm" variant="ghost" onClick={() => { setEditing(category); setFormOpen(true); }}><Pencil className="h-4 w-4" />تعديل</Button><Button size="sm" variant="ghost" className={category.active ? "text-red-700" : "text-emerald-700"} onClick={() => { setActionError(null); setStatusTarget(category); }}>{category.active ? "إيقاف" : "تفعيل"}</Button></div></td></tr>)}</tbody></DataTableWrapper>}
      <CategoryFormDialog open={formOpen} category={editing} onOpenChange={setFormOpen} />
      <ConfirmationDialog open={Boolean(statusTarget)} title={statusTarget?.active ? "إيقاف التصنيف" : "تفعيل التصنيف"} description={statusTarget?.active ? "لن يظهر التصنيف عند تسجيل مصروفات جديدة، وستبقى السجلات السابقة محفوظة." : "سيظهر التصنيف مجددًا عند تسجيل المصروفات."} confirmLabel={statusTarget?.active ? "إيقاف" : "تفعيل"} destructive={Boolean(statusTarget?.active)} busy={statusMutation.isPending} onOpenChange={(open) => !open && setStatusTarget(null)} onConfirm={() => statusTarget && statusMutation.mutate(statusTarget)} />
    </section>
  );
}
