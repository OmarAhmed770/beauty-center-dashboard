import { AlertTriangle, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

export function ErrorState({
  error,
  title = "تعذر إكمال الطلب",
  onRetry,
}: {
  error?: unknown;
  title?: string;
  onRetry?: () => void;
}) {
  const message = error ? getArabicApiErrorMessage(error) : "حصلت مشكلة، جرّب تاني";
  return (
    <div className="flex min-h-56 w-full flex-col items-center justify-center rounded-2xl border border-danger/25 bg-danger/10 p-8 text-center" role="alert">
      <span className="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-danger/15 text-danger"><AlertTriangle aria-hidden="true" /></span>
      <h3 className="font-black text-ink">{title}</h3>
      <p className="mt-2 max-w-lg text-sm leading-6 text-danger">{message}</p>
      {onRetry && <Button className="mt-5" variant="outline" onClick={onRetry}><RotateCcw className="h-4 w-4" />إعادة المحاولة</Button>}
    </div>
  );
}
