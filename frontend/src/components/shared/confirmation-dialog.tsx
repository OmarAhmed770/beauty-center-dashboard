import { AlertCircle, X } from "lucide-react";
import { useEffect, useRef } from "react";
import { Button } from "@/components/ui/button";

export function ConfirmationDialog({
  open,
  title,
  description,
  confirmLabel = "تأكيد",
  cancelLabel = "رجوع",
  destructive = false,
  busy = false,
  onConfirm,
  onOpenChange,
}: {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
  busy?: boolean;
  onConfirm: () => void;
  onOpenChange: (open: boolean) => void;
}) {
  const cancelButton = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    cancelButton.current?.focus();
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !busy) onOpenChange(false);
    };
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [busy, onOpenChange, open]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[70] flex items-end justify-center bg-stone-950/45 p-0 sm:grid sm:place-items-center sm:p-4" onMouseDown={() => !busy && onOpenChange(false)}>
      <section className="w-full max-w-md rounded-t-3xl bg-white p-5 shadow-2xl sm:rounded-2xl sm:p-6" role="alertdialog" aria-modal="true" aria-labelledby="confirmation-title" aria-describedby="confirmation-description" onMouseDown={(event) => event.stopPropagation()}>
        <div className="flex items-start justify-between gap-4">
          <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-amber-100 text-amber-800"><AlertCircle aria-hidden="true" /></span>
          <button type="button" className="grid h-11 w-11 place-items-center rounded-xl text-stone-500 hover:bg-stone-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500" aria-label="إغلاق" onClick={() => onOpenChange(false)} disabled={busy}><X className="h-5 w-5" /></button>
        </div>
        <h2 id="confirmation-title" className="mt-5 text-xl font-black text-stone-950">{title}</h2>
        <p id="confirmation-description" className="mt-2 text-sm leading-7 text-stone-600">{description}</p>
        <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button ref={cancelButton} className="w-full sm:w-auto" variant="outline" onClick={() => onOpenChange(false)} disabled={busy}>{cancelLabel}</Button>
          <Button className="w-full sm:w-auto" variant={destructive ? "destructive" : "default"} onClick={onConfirm} disabled={busy}>{busy ? "جارٍ التنفيذ..." : confirmLabel}</Button>
        </div>
      </section>
    </div>
  );
}
