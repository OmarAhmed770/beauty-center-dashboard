import { Inbox } from "lucide-react";
import type { ReactNode } from "react";

export function EmptyState({
  title = "لا توجد بيانات",
  description = "لم يتم العثور على سجلات لعرضها حاليًا.",
  action,
}: {
  title?: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex min-h-56 flex-col items-center justify-center rounded-2xl border border-dashed border-stone-300 bg-white p-8 text-center">
      <span className="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-stone-100 text-stone-500"><Inbox aria-hidden="true" /></span>
      <h3 className="font-black text-stone-900">{title}</h3>
      <p className="mt-2 max-w-md text-sm leading-6 text-stone-500">{description}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
