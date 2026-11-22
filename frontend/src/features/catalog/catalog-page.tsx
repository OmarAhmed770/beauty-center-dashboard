import { Pencil, Plus, Search } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
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
import { catalogKeys, searchCatalogCategories, searchCatalogItems, setCatalogItemActive } from "@/features/catalog/api";
import { CatalogItemFormDialog } from "@/features/catalog/catalog-item-form-dialog";
import { PriceUpdateDialog } from "@/features/catalog/price-update-dialog";
import type { CatalogItem, CatalogItemType } from "@/features/catalog/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { formatEgp } from "@/lib/format";

export function CatalogPage() {
  const { hasPermission } = useAuth();
  const canManage = hasPermission("SERVICES_AND_PRICES");
  const queryClient = useQueryClient();
  const [draftQuery, setDraftQuery] = useState("");
  const [query, setQuery] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [itemType, setItemType] = useState<CatalogItemType | "">("");
  const [active, setActive] = useState<"" | "true" | "false">("");
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<CatalogItem | null>(null);
  const [priceTarget, setPriceTarget] = useState<CatalogItem | null>(null);
  const [statusTarget, setStatusTarget] = useState<CatalogItem | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const categoriesQuery = useQuery({ queryKey: catalogKeys.categories({ size: 100 }), queryFn: () => searchCatalogCategories({ page: 0, size: 100 }), staleTime: 5 * 60_000 });
  const params = { query: query || undefined, categoryId: categoryId ? Number(categoryId) : undefined, itemType: itemType || undefined, active: active === "" ? undefined : active === "true", page, size: 20 };
  const itemsQuery = useQuery({ queryKey: catalogKeys.items(params), queryFn: () => searchCatalogItems(params) });
  const statusMutation = useMutation({ mutationFn: (item: CatalogItem) => setCatalogItemActive(item.id, !item.active), onSuccess: async () => { setStatusTarget(null); setActionError(null); await queryClient.invalidateQueries({ queryKey: catalogKeys.all }); }, onError: (error) => setActionError(getArabicApiErrorMessage(error)) });
  const submitSearch = (event: FormEvent) => { event.preventDefault(); setQuery(draftQuery.trim()); setPage(0); };
  return (
    <div className="space-y-6">
      <PageHeader title="الخدمات والأسعار" description="عدّل السعر أو أوقف الخدمة من مكان واحد. التفاصيل المتقدمة موجودة تحت «متقدم»." actions={canManage && <Button size="lg" onClick={() => { setEditing(null); setFormOpen(true); }}><Plus className="h-5 w-5" />إضافة خدمة</Button>} />
      <details className="rounded-2xl border border-[#eee7ea] bg-white px-4 shadow-sm">
        <summary className="flex min-h-13 cursor-pointer list-none items-center gap-2 text-sm font-black text-[#736873]"><Search className="h-4 w-4" />بحث وتصفية</summary>
        <form className="grid gap-3 border-t border-[#f1ebee] py-4 lg:grid-cols-[1fr_190px_170px_150px_auto]" onSubmit={submitSearch}>
          <label className="relative"><span className="sr-only">بحث</span><Search className="absolute right-3.5 top-3.5 h-5 w-5 text-stone-400" /><Input className="pr-11" value={draftQuery} onChange={(event) => setDraftQuery(event.target.value)} placeholder="اسم الخدمة أو الرمز" /></label>
          <Select aria-label="التصنيف" value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setPage(0); }}><option value="">كل التصنيفات</option>{categoriesQuery.data?.content.map((category) => <option key={category.id} value={category.id}>{category.nameArabic}</option>)}</Select>
          <Select aria-label="نوع الصنف" value={itemType} onChange={(event) => { setItemType(event.target.value as CatalogItemType | ""); setPage(0); }}><option value="">كل الأنواع</option><option value="SERVICE">خدمة</option><option value="PRODUCT">منتج</option><option value="HAIR_PRODUCT">منتج شعر</option><option value="HAIR_INSTALLATION">تركيب شعر</option></Select>
          <Select aria-label="الحالة" value={active} onChange={(event) => { setActive(event.target.value as typeof active); setPage(0); }}><option value="">كل الحالات</option><option value="true">نشط</option><option value="false">غير نشط</option></Select>
          <Button type="submit" variant="secondary"><Search className="h-4 w-4" />بحث</Button>
        </form>
      </details>
      {actionError && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-bold text-red-800">{actionError}</div>}
      {itemsQuery.isPending ? <LoadingState label="جارٍ تحميل الخدمات..." /> : itemsQuery.isError ? <ErrorState error={itemsQuery.error} onRetry={() => void itemsQuery.refetch()} /> : itemsQuery.data.content.length === 0 ? <EmptyState title="لا توجد خدمات مطابقة" description="أضف أول خدمة لتبدأ." /> : <><DataTableWrapper label="الخدمات والأسعار"><thead className={tableStyles.head}><tr><th>الخدمة</th><th>سعر البيع</th><th>الحالة</th><th>الإجراءات</th></tr></thead><tbody className={tableStyles.body}>{itemsQuery.data.content.map((item) => <tr key={item.id}><td><p className="font-black text-stone-950">{item.nameArabic}</p><p className="mt-1 font-mono text-xs text-stone-400" dir="ltr">{item.code}</p></td><td className="font-black tabular-nums">{formatEgp(item.currentSellingPrice)}</td><td><StatusBadge tone={item.active ? "success" : "neutral"}>{item.active ? "نشط" : "غير نشط"}</StatusBadge></td><td><div className="flex flex-wrap gap-1">{canManage && <><Button size="sm" variant="ghost" onClick={() => { setEditing(item); setFormOpen(true); }}><Pencil className="h-4 w-4" />تعديل</Button><Button size="sm" variant="ghost" onClick={() => setPriceTarget(item)}>تعديل السعر</Button><Button size="sm" variant="ghost" className={item.active ? "text-red-700" : "text-emerald-700"} onClick={() => setStatusTarget(item)}>{item.active ? "إيقاف" : "تفعيل"}</Button></>}</div></td></tr>)}</tbody></DataTableWrapper><PaginationControls page={itemsQuery.data.page} totalPages={itemsQuery.data.totalPages} totalElements={itemsQuery.data.totalElements} onPageChange={setPage} /></>}
      <CatalogItemFormDialog open={formOpen} item={editing} onOpenChange={setFormOpen} />
      <PriceUpdateDialog open={Boolean(priceTarget)} item={priceTarget} onOpenChange={(open) => !open && setPriceTarget(null)} />
      <ConfirmationDialog open={Boolean(statusTarget)} title={statusTarget?.active ? "إيقاف الصنف" : "تفعيل الصنف"} description="لن تتغير بنود الفواتير السابقة؛ يتأثر فقط استخدام الصنف في السجلات الجديدة." confirmLabel={statusTarget?.active ? "إيقاف" : "تفعيل"} destructive={Boolean(statusTarget?.active)} busy={statusMutation.isPending} onOpenChange={(open) => !open && setStatusTarget(null)} onConfirm={() => statusTarget && statusMutation.mutate(statusTarget)} />
    </div>
  );
}
