import { ArrowRight, Pencil, Plus } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { catalogKeys, searchCatalogCategories, setCatalogCategoryActive } from "@/features/catalog/api";
import { CatalogCategoryFormDialog } from "@/features/catalog/category-form-dialog";
import type { CatalogCategory } from "@/features/catalog/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

export function CatalogCategoriesPage() {
  const queryClient = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<CatalogCategory | null>(null);
  const [statusTarget, setStatusTarget] = useState<CatalogCategory | null>(null);
  const [error, setError] = useState<string | null>(null);
  const query = useQuery({ queryKey: catalogKeys.categories({ size: 100 }), queryFn: () => searchCatalogCategories({ page: 0, size: 100 }) });
  const statusMutation = useMutation({ mutationFn: (category: CatalogCategory) => setCatalogCategoryActive(category.id, !category.active), onSuccess: async () => { setStatusTarget(null); setError(null); await queryClient.invalidateQueries({ queryKey: catalogKeys.all }); }, onError: (caught) => setError(getArabicApiErrorMessage(caught)) });
  return <div className="space-y-6"><PageHeader title="تصنيفات الكتالوج" description="إدارة التصنيفات المرجعية للخدمات والمنتجات." actions={<><Button asChild variant="outline"><Link to="/catalog"><ArrowRight className="h-4 w-4" />الكتالوج</Link></Button><Button onClick={() => { setEditing(null); setFormOpen(true); }}><Plus className="h-4 w-4" />إضافة تصنيف</Button></>} />{error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-bold text-red-800">{error}</div>}{query.isPending ? <LoadingState label="جارٍ تحميل التصنيفات..." /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : query.data.content.length === 0 ? <EmptyState title="لا توجد تصنيفات" /> : <DataTableWrapper label="تصنيفات الكتالوج"><thead className={tableStyles.head}><tr><th>الاسم العربي</th><th>الاسم الإنجليزي</th><th>الوصف</th><th>الحالة</th><th>الإجراءات</th></tr></thead><tbody className={tableStyles.body}>{query.data.content.map((category) => <tr key={category.id}><td className="font-black text-stone-900">{category.nameArabic}</td><td dir="ltr" className="text-right">{category.nameEnglish}</td><td>{category.description || "—"}</td><td><StatusBadge tone={category.active ? "success" : "neutral"}>{category.active ? "نشط" : "غير نشط"}</StatusBadge></td><td><div className="flex gap-2"><Button size="sm" variant="ghost" onClick={() => { setEditing(category); setFormOpen(true); }}><Pencil className="h-4 w-4" />تعديل</Button><Button size="sm" variant="ghost" className={category.active ? "text-red-700" : "text-emerald-700"} onClick={() => { setError(null); setStatusTarget(category); }}>{category.active ? "إيقاف" : "تفعيل"}</Button></div></td></tr>)}</tbody></DataTableWrapper>}<CatalogCategoryFormDialog open={formOpen} category={editing} onOpenChange={setFormOpen} /><ConfirmationDialog open={Boolean(statusTarget)} title={statusTarget?.active ? "إيقاف التصنيف" : "تفعيل التصنيف"} description="لن تتغير بيانات الخدمات والمنتجات التاريخية، ويظل الخادم مسؤولًا عن منع استخدام التصنيف غير النشط." confirmLabel={statusTarget?.active ? "إيقاف" : "تفعيل"} destructive={Boolean(statusTarget?.active)} busy={statusMutation.isPending} onOpenChange={(open) => !open && setStatusTarget(null)} onConfirm={() => statusTarget && statusMutation.mutate(statusTarget)} /></div>;
}
