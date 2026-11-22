import { Plus, Trash2 } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { FormServerError } from "@/components/shared/form-field";
import { MoneyInput } from "@/components/shared/money-input";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { paymentMethodKeys, searchPaymentMethods } from "@/features/payment-methods/api";
import { invoiceKeys, setInvoicePayments } from "@/features/sales/api";
import { paymentTotals } from "@/features/sales/model";
import type { InvoiceDetails, InvoicePaymentInput } from "@/features/sales/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { formatEgp } from "@/lib/format";

const moneyPattern = /^\d{1,17}(\.\d{1,2})?$/;

export function PaymentEditor({ invoice, payments, onChange, onValidityChange }: { invoice: InvoiceDetails; payments: InvoicePaymentInput[]; onChange: (payments: InvoicePaymentInput[]) => void; onValidityChange: (valid: boolean) => void }) {
  const queryClient = useQueryClient();
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const methodsQuery = useQuery({ queryKey: [...paymentMethodKeys.all, "invoice-active"], queryFn: () => searchPaymentMethods({ active: true, page: 0, size: 100 }), staleTime: 60_000 });
  const balance = paymentTotals(invoice.finalTotal, payments);
  const validation = useMemo(() => {
    if (payments.length === 0) return "أضف طريقة دفع واحدة على الأقل.";
    const seen = new Set<number>();
    for (const payment of payments) {
      if (!payment.paymentMethodId) return "اختر طريقة الدفع لكل صف.";
      if (seen.has(payment.paymentMethodId)) return "لا يمكن تكرار طريقة الدفع في نفس الفاتورة.";
      seen.add(payment.paymentMethodId);
      if (!moneyPattern.test(payment.amount) || Number(payment.amount) <= 0) return "يجب أن يكون كل مبلغ دفع أكبر من صفر وبمنزلتين عشريتين كحد أقصى.";
      const method = methodsQuery.data?.content.find((candidate) => candidate.id === payment.paymentMethodId);
      if (method?.requiresReferenceNumber && !payment.referenceNumber?.trim()) return `مرجع الدفع مطلوب لطريقة ${method.displayNameArabic}.`;
    }
    return null;
  }, [methodsQuery.data?.content, payments]);
  const valid = !validation && balance.matches;
  useEffect(() => onValidityChange(valid), [onValidityChange, valid]);
  const mutation = useMutation({ mutationFn: () => setInvoicePayments(invoice.id, payments.map((payment) => ({ ...payment, referenceNumber: payment.referenceNumber?.trim() || undefined }))), onSuccess: async (updated) => { queryClient.setQueryData(invoiceKeys.detail(invoice.id), updated); await queryClient.invalidateQueries({ queryKey: invoiceKeys.preview(invoice.id) }); setSaveMessage("تم حفظ توزيع الدفعات في المسودة."); }, onError: (error) => setSaveMessage(getArabicApiErrorMessage(error)) });
  const update = (index: number, patch: Partial<InvoicePaymentInput>) => { setSaveMessage(null); onChange(payments.map((payment, current) => current === index ? { ...payment, ...patch } : payment)); };
  const add = () => { const used = new Set(payments.map((payment) => payment.paymentMethodId)); const method = methodsQuery.data?.content.find((candidate) => !used.has(candidate.id)); if (!method) { setSaveMessage("لا توجد طريقة دفع نشطة أخرى متاحة."); return; } const suggested = balance.remaining > 0 ? balance.remaining.toFixed(2) : ""; onChange([...payments, { paymentMethodId: method.id, amount: suggested, referenceNumber: "" }]); setSaveMessage(null); };
  return <Card className="p-4 sm:p-5"><div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-start sm:justify-between"><div><h2 className="text-lg font-black text-stone-950">طرق الدفع</h2><p className="mt-1 text-sm text-stone-500">يمكن تقسيم المبلغ على أكثر من طريقة دون تكرار الطريقة.</p></div><Button type="button" className="w-full sm:w-auto" variant="outline" onClick={add} disabled={methodsQuery.isPending || payments.length >= 20}><Plus className="h-4 w-4" />إضافة طريقة</Button></div><div className="mt-5 space-y-3">{payments.map((payment, index) => { const method = methodsQuery.data?.content.find((candidate) => candidate.id === payment.paymentMethodId); return <div key={`${index}-${payment.paymentMethodId}`} className="grid gap-3 rounded-xl border border-stone-200 p-3 lg:grid-cols-[1.1fr_1fr_1.2fr_auto]"><Select aria-label={`طريقة الدفع ${index + 1}`} value={payment.paymentMethodId || ""} onChange={(event) => update(index, { paymentMethodId: Number(event.target.value), referenceNumber: "" })}><option value="">اختر طريقة الدفع</option>{methodsQuery.data?.content.map((candidate) => <option key={candidate.id} value={candidate.id} disabled={payments.some((row, rowIndex) => rowIndex !== index && row.paymentMethodId === candidate.id)}>{candidate.displayNameArabic}</option>)}</Select><MoneyInput aria-label={`مبلغ الدفع ${index + 1}`} value={payment.amount} onChange={(event) => update(index, { amount: event.target.value })} /><Input aria-label={`مرجع الدفع ${index + 1}`} value={payment.referenceNumber ?? ""} onChange={(event) => update(index, { referenceNumber: event.target.value })} placeholder={method?.requiresReferenceNumber ? "المرجع مطلوب" : "مرجع اختياري"} /><Button type="button" size="icon" variant="ghost" className="justify-self-end text-red-700 lg:justify-self-auto" aria-label={`حذف طريقة الدفع ${index + 1}`} onClick={() => onChange(payments.filter((_, current) => current !== index))}><Trash2 className="h-4 w-4" /></Button></div>; })}{payments.length === 0 && <p className="rounded-xl bg-stone-50 p-4 text-sm text-stone-500">لم تُضف دفعات بعد.</p>}</div><div className="mt-5 grid gap-3 border-t border-stone-200 pt-5 sm:grid-cols-3"><Total label="إجمالي الفاتورة" value={balance.total} /><Total label="المبلغ المدفوع" value={balance.paid} /><Total label={balance.remaining < 0 ? "الزيادة" : "المتبقي"} value={Math.abs(balance.remaining)} alert={!balance.matches} /></div>{validation && <p className="mt-3 text-sm font-bold text-red-700" role="alert">{validation}</p>}{!validation && !balance.matches && <p className="mt-3 text-sm font-bold text-amber-700" role="alert">يجب أن يساوي مجموع الدفعات إجمالي الفاتورة قبل الإكمال.</p>}<div className="mt-5 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center"><Button type="button" className="w-full sm:w-auto" variant="secondary" disabled={mutation.isPending || Boolean(validation)} onClick={() => mutation.mutate()}>{mutation.isPending ? "جارٍ حفظ الدفعات..." : "حفظ توزيع الدفعات"}</Button>{saveMessage && <span className={mutation.isError ? "text-sm font-bold text-red-700" : "text-sm font-bold text-emerald-700"}>{saveMessage}</span>}</div>{methodsQuery.isError && <div className="mt-3"><FormServerError message={getArabicApiErrorMessage(methodsQuery.error)} /></div>}</Card>;
}

function Total({ label, value, alert = false }: { label: string; value: number; alert?: boolean }) { return <div><p className="text-xs font-bold text-stone-500">{label}</p><p className={`mt-2 text-xl font-black tabular-nums ${alert ? "text-amber-700" : "text-stone-950"}`}>{formatEgp(value)}</p></div>; }
