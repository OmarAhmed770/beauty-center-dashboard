import { EmptyState } from "@/components/shared/empty-state";
import type { DailySalesTrend } from "@/features/reports/types";
import { formatArabicDate, formatEgp } from "@/lib/format";

export function DailySalesChart({ rows }: { rows: DailySalesTrend[] }) {
  if (rows.length === 0) return <EmptyState title="لا توجد مبيعات في الفترة" description="سيظهر اتجاه المبيعات عند اكتمال فواتير ضمن الفترة المحددة." />;
  const maximum = Math.max(...rows.map((row) => Number(row.netSales)), 1);
  return (
    <div className="overflow-x-auto" role="img" aria-label="رسم أعمدة لصافي المبيعات اليومية">
      <div className="flex min-w-max items-end gap-3 border-b border-stone-200 px-2 pt-8" style={{ minHeight: 260 }}>
        {rows.map((row) => {
          const height = Math.max(6, Math.round((Number(row.netSales) / maximum) * 170));
          return (
            <div key={row.businessDate} className="group flex w-20 flex-col items-center justify-end gap-2 text-center">
              <span className="text-[10px] font-bold text-stone-500 opacity-0 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100">{formatEgp(row.netSales)}</span>
              <div className="w-10 rounded-t-lg bg-gradient-to-t from-rose-800 to-rose-400" style={{ height }} title={`${formatArabicDate(row.businessDate)}: ${formatEgp(row.netSales)}`} />
              <span className="pb-2 text-[10px] font-bold text-stone-500">{formatArabicDate(row.businessDate)}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
