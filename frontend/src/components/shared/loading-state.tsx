import { LoaderCircle } from "lucide-react";
import { cn } from "@/lib/utils";

export function LoadingState({ label = "جارٍ التحميل...", fullPage = false }: { label?: string; fullPage?: boolean }) {
  return (
    <div
      className={cn(
        "flex min-h-52 flex-col items-center justify-center gap-3 rounded-2xl border border-stone-200 bg-white p-8 text-stone-600",
        fullPage && "min-h-screen rounded-none border-0 bg-stone-50",
      )}
      role="status"
    >
      <LoaderCircle className="h-8 w-8 animate-spin text-rose-700" aria-hidden="true" />
      <p className="text-sm font-bold">{label}</p>
    </div>
  );
}
