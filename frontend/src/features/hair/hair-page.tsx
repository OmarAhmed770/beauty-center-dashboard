import { ArrowRight, PackagePlus, Plus, Search } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { PaginationControls } from "@/components/shared/pagination-controls";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { useAuth } from "@/features/auth/auth-context";
import { hairKeys, searchHairConfigurations, setHairConfigurationActive } from "@/features/hair/api";
import { HairConfigurationFormDialog } from "@/features/hair/hair-configuration-form-dialog";
import { HairStockDialog } from "@/features/hair/hair-stock-dialog";
import { HairPreviewPanel } from "@/features/hair/hair-preview-panel";
import type { HairConfiguration } from "@/features/hair/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { formatArabicDate, formatMoney } from "@/lib/format";

export function HairPage() {
  const { hasPermission, hasAnyRole } = useAuth();
  const canManage = hasPermission("SERVICES_AND_PRICES");
  const canManageStock = hasAnyRole(["OWNER"]);
  const queryClient = useQueryClient();
  const [draftQuery, setDraftQuery] = useState("");
  const [query, setQuery] = useState("");
  const [active, setActive] = useState<"" | "true" | "false">("");
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [statusTarget, setStatusTarget] = useState<HairConfiguration | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [stockTarget, setStockTarget] = useState<HairConfiguration | null>(null);
  const params = { query: query || undefined, active: active === "" ? undefined : active === "true", page, size: 20 };
  const queryResult = useQuery({ queryKey: hairKeys.list(params), queryFn: () => searchHairConfigurations(params) });
  const statusMutation = useMutation({ mutationFn: (configuration: HairConfiguration) => setHairConfigurationActive(configuration.id, !configuration.active), onSuccess: async () => { setStatusTarget(null); setActionError(null); await queryClient.invalidateQueries({ queryKey: hairKeys.all }); }, onError: (error) => setActionError(getArabicApiErrorMessage(error)) });
  const submit = (event: FormEvent) => { event.preventDefault(); setQuery(draftQuery.trim()); setPage(0); };

  return <div className="space-y-7"><PageHeader title="تركيب الشعر" actions={<><Button asChild variant="outline"><Link to="/catalog"><ArrowRight className="h-4 w-4" />الخدمات والأسعار</Link></Button>{canManage && <Button onClick={() => setFormOpen(true)}><Plus className="h-4 w-4" />إعداد جديد</Button>}</>} /><HairPreviewPanel onAddConfiguration={canManage ? () => setFormOpen(true) : undefined} /><section className="space-y-4"><h2 className="text-lg font-black text-ink">إعدادات الشعر</h2><form className="grid gap-3 rounded-2xl border border-soft-border bg-white p-4 shadow-sm md:grid-cols-[1fr_190px_auto]" onSubmit={submit}><label className="relative"><span className="sr-only">بحث</span><Search className="absolute right-3.5 top-3.5 h-5 w-5 text-muted" /><Input className="pr-11" value={draftQuery} onChange={(event) => setDraftQuery(event.target.value)} placeholder="المنتج أو العلامة أو الطول أو اللون" /></label><Select aria-label="حالة الإعداد" value={active} onChange={(event) => { setActive(event.target.value as typeof active); setPage(0); }}><option value="">كل الحالات</option><option value="true">نشط</option><option value="false">غير نشط</option></Select><Button type="submit" variant="secondary"><Search className="h-4 w-4" />بحث</Button></form>{actionError && <div role="alert" className="rounded-xl border border-danger/20 bg-danger/10 p-3 text-sm font-bold text-danger">{actionError}</div>}{queryResult.isPending ? <LoadingState label="جارٍ تحميل الإعدادات..." /> : queryResult.isError ? <ErrorState error={queryResult.error} onRetry={() => void queryResult.refetch()} /> : queryResult.data.content.length === 0 ? null : <><DataTableWrapper label="إعدادات تركيب الشعر"><thead className={tableStyles.head}><tr><th>منتج الشعر</th><th>الخصائص</th><th>سعر البيع للجرام</th>{canManage && <th>سعر الشراء للجرام</th>}<th>رسوم التركيب</th>{canManageStock && <th>المخزون</th>}<th>السريان</th><th>الحالة</th><th>الإجراء</th></tr></thead><tbody className={tableStyles.body}>{queryResult.data.content.map((configuration) => <tr key={configuration.id}><td><p className="font-black text-ink">{configuration.hairItemNameArabic}</p><p className="mt-1 font-mono text-xs text-muted" dir="ltr">{configuration.hairItemCode}</p></td><td><p>{configuration.brand} — {configuration.length}</p><p className="mt-1 text-xs text-muted">{configuration.color || "بدون لون"} · {configuration.unitType}</p></td><td className="font-bold tabular-nums"><bdi dir="ltr">{formatMoney(configuration.unitSellingPrice)}</bdi></td>{canManage && <td className="tabular-nums"><bdi dir="ltr">{formatMoney(configuration.unitCost)}</bdi></td>}<td className="tabular-nums"><bdi dir="ltr">{formatMoney(configuration.installationFee)}</bdi></td>{canManageStock && <td><bdi dir="ltr" className="font-bold">{configuration.stockConfigured ? `${configuration.availableStockGrams ?? 0} جرام` : "غير محدد"}</bdi></td>}<td>{formatArabicDate(configuration.effectiveDate)}</td><td><StatusBadge tone={configuration.active ? "success" : "neutral"}>{configuration.active ? "نشط" : "غير نشط"}</StatusBadge></td><td className="space-x-1 space-x-reverse">{canManageStock && <Button size="sm" variant="ghost" onClick={() => setStockTarget(configuration)}><PackagePlus className="h-4 w-4" />مخزون</Button>}{canManage && <Button size="sm" variant="ghost" className={configuration.active ? "text-danger" : "text-success"} onClick={() => setStatusTarget(configuration)}>{configuration.active ? "إيقاف" : "تفعيل"}</Button>}</td></tr>)}</tbody></DataTableWrapper><PaginationControls page={queryResult.data.page} totalPages={queryResult.data.totalPages} totalElements={queryResult.data.totalElements} onPageChange={setPage} /></>}</section><HairConfigurationFormDialog open={formOpen} onOpenChange={setFormOpen} /><HairStockDialog configuration={stockTarget} open={Boolean(stockTarget)} onOpenChange={(open) => !open && setStockTarget(null)} /><ConfirmationDialog open={Boolean(statusTarget)} title={statusTarget?.active ? "إيقاف إعداد الشعر" : "تفعيل إعداد الشعر"} description="يبقى الإعداد محفوظًا للتاريخ، ولا يمكن استخدام غير النشط في معاينة أو فاتورة جديدة." confirmLabel={statusTarget?.active ? "إيقاف" : "تفعيل"} destructive={Boolean(statusTarget?.active)} busy={statusMutation.isPending} onOpenChange={(open) => !open && setStatusTarget(null)} onConfirm={() => statusTarget && statusMutation.mutate(statusTarget)} /></div>;
}
