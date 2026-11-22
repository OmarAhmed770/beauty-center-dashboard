import { CreditCard, Pencil, Plus } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { paymentMethodTypeLabels } from "@/features/expenses/model";
import { paymentMethodKeys, searchPaymentMethods, setPaymentMethodActive } from "@/features/payment-methods/api";
import { PaymentMethodFormDialog } from "@/features/payment-methods/payment-method-form-dialog";
import type { PaymentMethod } from "@/features/payment-methods/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

export function PaymentMethodManagement() {
  const queryClient = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<PaymentMethod | null>(null);
  const [statusTarget, setStatusTarget] = useState<PaymentMethod | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const query = useQuery({ queryKey: [...paymentMethodKeys.all, "management"], queryFn: () => searchPaymentMethods({ page: 0, size: 100 }) });
  const statusMutation = useMutation({
    mutationFn: (method: PaymentMethod) => setPaymentMethodActive(method.id, !method.active),
    onSuccess: async () => { setStatusTarget(null); setActionError(null); await queryClient.invalidateQueries({ queryKey: paymentMethodKeys.all }); },
    onError: (error) => setActionError(getArabicApiErrorMessage(error)),
  });

  return (
    <section className="space-y-4" aria-labelledby="payment-management-heading">
      <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 id="payment-management-heading" className="flex items-center gap-2 text-lg font-black text-stone-950"><CreditCard className="text-rose-700" />طرق الدفع</h2><p className="mt-1 text-sm text-stone-500">إعداد الطرق المتاحة ومتطلبات الرقم المرجعي.</p></div><Button onClick={() => { setEditing(null); setFormOpen(true); }}><Plus className="h-4 w-4" />إضافة طريقة</Button></div>
      {actionError && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-bold text-red-800" role="alert">{actionError}</div>}
      {query.isPending ? <LoadingState label="جارٍ تحميل طرق الدفع..." /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : query.data.content.length === 0 ? <EmptyState title="لا توجد طرق دفع" /> : <DataTableWrapper label="طرق الدفع"><thead className={tableStyles.head}><tr><th>الاسم</th><th>الرمز</th><th>النوع</th><th>المرجع</th><th>الترتيب</th><th>الحالة</th><th>الإجراءات</th></tr></thead><tbody className={tableStyles.body}>{query.data.content.map((method) => <tr key={method.id}><td className="font-black text-stone-900">{method.displayNameArabic}</td><td dir="ltr" className="text-right font-mono">{method.code}</td><td>{paymentMethodTypeLabels[method.type]}</td><td>{method.requiresReferenceNumber ? "مطلوب" : "اختياري"}</td><td>{method.sortOrder}</td><td><StatusBadge tone={method.active ? "success" : "neutral"}>{method.active ? "نشطة" : "غير نشطة"}</StatusBadge></td><td><div className="flex gap-2"><Button size="sm" variant="ghost" onClick={() => { setEditing(method); setFormOpen(true); }}><Pencil className="h-4 w-4" />تعديل</Button><Button size="sm" variant="ghost" className={method.active ? "text-red-700" : "text-emerald-700"} onClick={() => { setActionError(null); setStatusTarget(method); }}>{method.active ? "إيقاف" : "تفعيل"}</Button></div></td></tr>)}</tbody></DataTableWrapper>}
      <PaymentMethodFormDialog open={formOpen} paymentMethod={editing} onOpenChange={setFormOpen} />
      <ConfirmationDialog open={Boolean(statusTarget)} title={statusTarget?.active ? "إيقاف طريقة الدفع" : "تفعيل طريقة الدفع"} description={statusTarget?.active ? "لن تتاح الطريقة في المصروفات الجديدة، مع بقاء السجلات السابقة محفوظة." : "ستتاح الطريقة مجددًا عند تسجيل المصروفات."} confirmLabel={statusTarget?.active ? "إيقاف" : "تفعيل"} destructive={Boolean(statusTarget?.active)} busy={statusMutation.isPending} onOpenChange={(open) => !open && setStatusTarget(null)} onConfirm={() => statusTarget && statusMutation.mutate(statusTarget)} />
    </section>
  );
}
