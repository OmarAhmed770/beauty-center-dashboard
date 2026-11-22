import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { adjustHairStock, getHairStock, hairKeys, initializeHairStock } from "@/features/hair/api";
import type { HairConfiguration } from "@/features/hair/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { formatQuantity } from "@/lib/format";

export function HairStockDialog({ configuration, open, onOpenChange }: {
  configuration: HairConfiguration | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const queryClient = useQueryClient();
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string>();
  const stockQuery = useQuery({
    queryKey: configuration ? hairKeys.stock(configuration.hairItemId) : ["hair-stock", "closed"],
    queryFn: () => getHairStock(configuration!.hairItemId),
    enabled: open && configuration !== null,
  });
  useEffect(() => {
    if (open) {
      setQuantity("");
      setReason("");
      setError(undefined);
    }
  }, [open, configuration?.id]);
  const mutation = useMutation({
    mutationFn: async () => {
      if (!configuration) throw new Error("لا يوجد منتج شعر محدد");
      if (!/^-?\d+$/.test(quantity.trim()) || (!stockQuery.data?.stockConfigured && Number(quantity) < 0)) {
        throw new Error("أدخل كمية صحيحة بالجرام");
      }
      if (!reason.trim()) throw new Error("اكتب سبب التعديل");
      return stockQuery.data?.stockConfigured
        ? adjustHairStock(configuration.hairItemId, { adjustmentGrams: quantity, reason: reason.trim() })
        : initializeHairStock(configuration.hairItemId, { quantityGrams: quantity, reason: reason.trim() });
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: hairKeys.all }),
        configuration ? queryClient.invalidateQueries({ queryKey: hairKeys.stock(configuration.hairItemId) }) : Promise.resolve(),
      ]);
      onOpenChange(false);
    },
    onError: (requestError) => setError(requestError instanceof Error && requestError.message.startsWith("أ")
      ? requestError.message
      : getArabicApiErrorMessage(requestError)),
  });
  const stock = stockQuery.data;
  const initialized = stock?.stockConfigured === true;
  return <DialogShell
    open={open}
    title="مخزون الشعر"
    description={configuration ? `${configuration.hairItemNameArabic} — الكمية بالجرام` : undefined}
    onOpenChange={onOpenChange}
    footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button type="submit" form="hair-stock-form" disabled={stockQuery.isPending || mutation.isPending}>{mutation.isPending ? "جارٍ الحفظ..." : initialized ? "حفظ التعديل" : "تحديد الرصيد"}</Button></>}
  >
    <form id="hair-stock-form" className="space-y-5" onSubmit={(event) => { event.preventDefault(); mutation.mutate(); }} noValidate>
      {stockQuery.isPending ? <p className="text-sm font-bold text-muted">جارٍ تحميل المخزون...</p> : <>
        <div className="rounded-2xl bg-primary-soft p-4"><p className="text-sm font-bold text-muted">المخزون الحالي</p><bdi dir="ltr" className="mt-1 block text-2xl font-black text-primary">{initialized ? `${formatQuantity(stock.availableGrams)} جرام` : "لم يتم تحديده"}</bdi></div>
        <FormField label={initialized ? "التعديل بالجرام" : "الرصيد الافتتاحي بالجرام"} htmlFor="hair-stock-quantity" required hint={initialized ? "اكتب عددًا صحيحًا موجبًا للإضافة أو سالبًا للخصم." : "اكتب عدد الجرامات الفعلي؛ لا يمكن تكرار الرصيد الافتتاحي."}>
          <Input id="hair-stock-quantity" type="number" min={initialized ? undefined : "0"} step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} inputMode="numeric" placeholder={initialized ? "مثال: 250 أو -50" : "مثال: 500"} className="text-lg font-bold" />
        </FormField>
        <FormField label="سبب التعديل" htmlFor="hair-stock-reason" required><Input id="hair-stock-reason" value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} placeholder="مثال: توريد جديد أو جرد فعلي" /></FormField>
      </>}
      {stockQuery.isError && <FormServerError message={getArabicApiErrorMessage(stockQuery.error)} />}
      <FormServerError message={error} />
    </form>
  </DialogShell>;
}
