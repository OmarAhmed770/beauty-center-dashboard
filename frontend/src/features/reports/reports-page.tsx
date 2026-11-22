import { ChevronDown } from "lucide-react";
import { Link } from "react-router-dom";
import { PageHeader } from "@/components/shared/page-header";

const reports = [
  ["/reports/daily", "التقرير اليومي"],
  ["/reports/financial", "التقرير المالي لفترة"],
  ["/reports/employees", "كشف حساب موظف"],
  ["/reports/expenses", "تقرير المصروفات"],
  ["/reports/sales", "تقرير المبيعات"],
  ["/reports/payments", "تقرير طرق الدفع"],
  ["/reports/profit", "تقرير الربح"],
  ["/reports/price-history", "سجل الأسعار"],
] as const;

/** Detailed reports stay available, but daily work starts from the unified dashboard. */
export function ReportsPage() {
  return <div className="mx-auto max-w-3xl space-y-6"><PageHeader title="تقارير متقدمة" description="ملخص اليوم والشهر موجود في الرئيسية. افتح التفاصيل فقط عند الحاجة." /><details className="group rounded-2xl border border-soft-border bg-white shadow-sm"><summary className="flex min-h-16 cursor-pointer list-none items-center justify-between gap-3 px-5 font-black text-ink"><span>فتح التقارير التفصيلية</span><ChevronDown className="h-5 w-5 text-primary transition-transform group-open:rotate-180" /></summary><nav className="grid gap-1 border-t border-soft-border p-3" aria-label="التقارير التفصيلية">{reports.map(([path, label]) => <Link key={path} to={path} className="rounded-xl px-4 py-3 text-sm font-bold text-ink hover:bg-primary-soft hover:text-primary">{label}</Link>)}</nav></details></div>;
}
