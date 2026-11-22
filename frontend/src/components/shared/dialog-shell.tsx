import { X } from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { cn } from "@/lib/utils";

export function DialogShell({
  open,
  title,
  description,
  children,
  footer,
  onOpenChange,
  size = "md",
}: {
  open: boolean;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
  onOpenChange: (open: boolean) => void;
  size?: "sm" | "md" | "lg" | "xl";
}) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onOpenChange(false);
    };
    document.addEventListener("keydown", onKeyDown);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = "";
    };
  }, [onOpenChange, open]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[70] flex items-end justify-center bg-stone-950/50 p-0 sm:items-center sm:p-4" onMouseDown={() => onOpenChange(false)}>
      <section
        className={cn(
          "flex h-[100dvh] max-h-[100dvh] w-full flex-col overflow-hidden rounded-none bg-white shadow-2xl sm:h-auto sm:max-h-[92vh] sm:rounded-[1.75rem]",
          size === "sm" && "max-w-md",
          size === "md" && "max-w-xl",
          size === "lg" && "max-w-3xl",
          size === "xl" && "max-w-5xl",
        )}
        role="dialog"
        aria-modal="true"
        aria-labelledby="dialog-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="flex shrink-0 items-start justify-between gap-4 border-b border-[#eee7ea] px-4 py-4 sm:px-6 sm:py-5">
          <div className="min-w-0"><h2 id="dialog-title" className="text-lg font-black text-[#29252A] sm:text-xl">{title}</h2>{description && <p className="mt-1.5 text-sm leading-6 text-[#736873]">{description}</p>}</div>
          <button type="button" className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl text-[#736873] hover:bg-[#FCEAF2] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A83868]" aria-label="إغلاق" onClick={() => onOpenChange(false)}><X className="h-5 w-5" /></button>
        </header>
        <div className="min-h-0 overflow-y-auto overscroll-contain px-4 py-5 sm:px-6">{children}</div>
        {footer && <footer className="flex shrink-0 flex-col-reverse gap-3 border-t border-[#eee7ea] bg-[#fff9fb] px-4 py-4 sm:flex-row sm:flex-wrap sm:justify-end sm:px-6 [&>button]:w-full sm:[&>button]:w-auto">{footer}</footer>}
      </section>
    </div>
  );
}
