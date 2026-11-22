import { useEffect, useState } from "react";
import { DialogShell } from "@/components/shared/dialog-shell";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

export function ReasonDialog({
  open,
  title,
  description,
  confirmLabel = "تأكيد الإلغاء",
  busy = false,
  error,
  onConfirm,
  onOpenChange,
}: {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  busy?: boolean;
  error?: string | null;
  onConfirm: (reason: string) => void;
  onOpenChange: (open: boolean) => void;
}) {
  const [reason, setReason] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setReason("");
      setValidationError(null);
    }
  }, [open]);

  const confirm = () => {
    const normalized = reason.trim();
    if (!normalized) {
      setValidationError("سبب الإلغاء مطلوب.");
      return;
    }
    if (normalized.length > 500) {
      setValidationError("يجب ألا يتجاوز سبب الإلغاء 500 حرف.");
      return;
    }
    onConfirm(normalized);
  };

  return (
    <DialogShell
      open={open}
      title={title}
      description={description}
      onOpenChange={onOpenChange}
      size="sm"
      footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={busy}>رجوع</Button><Button variant="destructive" onClick={confirm} disabled={busy}>{busy ? "جارٍ الإلغاء..." : confirmLabel}</Button></>}
    >
      <label className="block text-sm font-bold text-stone-700" htmlFor="cancellation-reason">سبب الإلغاء</label>
      <Textarea id="cancellation-reason" className="mt-2" maxLength={500} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="اكتب سببًا واضحًا لحفظه في سجل التدقيق" />
      <div className="mt-2 flex justify-between text-xs"><span className="font-semibold text-red-700" role={validationError || error ? "alert" : undefined}>{validationError || error}</span><span className="text-stone-400">{reason.length}/500</span></div>
    </DialogShell>
  );
}
