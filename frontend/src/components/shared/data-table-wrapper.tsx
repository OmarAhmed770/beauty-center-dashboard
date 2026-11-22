import { useEffect, useRef, type ReactNode } from "react";
import { cn } from "@/lib/utils";

export function DataTableWrapper({ children, label, className, mobileCards = true }: { children: ReactNode; label: string; className?: string; mobileCards?: boolean }) {
  const tableRef = useRef<HTMLTableElement>(null);

  useEffect(() => {
    if (!mobileCards || !tableRef.current) return;
    const labels = Array.from(tableRef.current.querySelectorAll("thead th")).map((heading) => heading.textContent?.trim() ?? "");
    tableRef.current.querySelectorAll("tbody tr").forEach((row) => {
      Array.from(row.children).forEach((cell, index) => {
        if (cell instanceof HTMLTableCellElement) cell.dataset.mobileLabel = labels[index] ?? "";
      });
    });
  }, [children, mobileCards]);

  return (
    <div className={cn("overflow-hidden rounded-[1.5rem] border border-soft-border bg-white/95 shadow-[0_12px_30px_rgb(58_37_48_/_0.06)]", mobileCards && "responsive-data-table", className)}>
      <div className="overflow-x-auto overscroll-x-contain">
        <table ref={tableRef} className="w-full text-sm" aria-label={label}>{children}</table>
      </div>
    </div>
  );
}

export const tableStyles = {
  head: "border-b border-soft-border bg-surface-soft text-right text-xs font-black text-muted [&>tr>th]:px-3 [&>tr>th]:py-3 sm:[&>tr>th]:px-5 sm:[&>tr>th]:py-4",
  body: "divide-y divide-soft-border text-ink [&>tr>td]:px-3 [&>tr>td]:py-3 sm:[&>tr>td]:px-5 sm:[&>tr>td]:py-4 [&>tr:hover]:bg-surface-soft",
};
