import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export function PaginationControls({
  page,
  totalPages,
  totalElements,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  totalElements?: number;
  onPageChange: (page: number) => void;
}) {
  const safeTotalPages = Math.max(totalPages, 1);
  return (
    <nav className="flex flex-col gap-3 rounded-xl border border-stone-200 bg-white px-4 py-3 sm:flex-row sm:items-center sm:justify-between" aria-label="التنقل بين الصفحات">
      <p className="text-sm text-stone-500">
        الصفحة <strong className="text-stone-800">{page + 1}</strong> من <strong className="text-stone-800">{safeTotalPages}</strong>
        {typeof totalElements === "number" && <span> — {totalElements} سجل</span>}
      </p>
      <div className="grid grid-cols-2 gap-2 sm:flex">
        <Button size="sm" className="w-full sm:w-auto" variant="outline" disabled={page <= 0} onClick={() => onPageChange(page - 1)}><ChevronRight className="h-4 w-4" />السابق</Button>
        <Button size="sm" className="w-full sm:w-auto" variant="outline" disabled={page + 1 >= safeTotalPages} onClick={() => onPageChange(page + 1)}>التالي<ChevronLeft className="h-4 w-4" /></Button>
      </div>
    </nav>
  );
}
