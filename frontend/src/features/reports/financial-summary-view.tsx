import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { EmptyState } from "@/components/shared/empty-state";
import { Card } from "@/components/ui/card";
import { DailySalesChart } from "@/features/reports/daily-sales-chart";
import type { FinancialSummary } from "@/features/reports/types";
import { catalogItemTypeLabels } from "@/features/catalog/model";
import { formatEgp, formatNumber, formatQuantity } from "@/lib/format";

const metricDefinitions: { key: keyof FinancialSummary; label: string; tone?: string; numeric?: boolean }[] = [
  { key: "grossSales", label: "إجمالي المبيعات" },
  { key: "discounts", label: "الخصومات", tone: "text-red-700" },
  { key: "refunds", label: "المبالغ المستردة", tone: "text-amber-700" },
  { key: "netSales", label: "صافي المبيعات", tone: "text-emerald-700" },
  { key: "completedInvoiceCount", label: "عدد الفواتير", numeric: true },
  { key: "expenses", label: "المصروفات", tone: "text-red-700" },
  { key: "employeeCost", label: "تكلفة الموظفين" },
  { key: "costOfSoldItemsAndHair", label: "تكلفة المنتجات والشعر" },
  { key: "netProfit", label: "صافي الربح", tone: "text-emerald-800" },
  { key: "expectedCashBalance", label: "النقدية المتوقعة", tone: "text-rose-800" },
];

export function FinancialSummaryView({ summary, compact = false }: { summary: FinancialSummary; compact?: boolean }) {
  return (
    <div className="space-y-6">
      <section className={compact ? "grid gap-3 sm:grid-cols-2 lg:grid-cols-5" : "grid gap-4 sm:grid-cols-2 xl:grid-cols-5"}>
        {metricDefinitions.map(({ key, label, tone, numeric }) => (
          <Card key={key} className={compact ? "p-4" : "p-5"}>
            <p className="text-xs font-bold text-stone-500">{label}</p>
            <p className={`mt-3 font-black tabular-nums ${compact ? "text-lg" : "text-2xl"} ${tone ?? "text-stone-950"}`}>
              {numeric ? formatNumber(summary[key] as number | string) : formatEgp(summary[key] as number | string)}
            </p>
          </Card>
        ))}
      </section>

      <section className="grid gap-5 xl:grid-cols-2">
        <Card className="p-5">
          <h2 className="mb-4 text-lg font-black text-stone-950">الإجماليات حسب طريقة الدفع</h2>
          {summary.paymentTotalsByMethod.length === 0 ? <EmptyState title="لا توجد دفعات" /> : <DataTableWrapper label="طرق الدفع"><thead className={tableStyles.head}><tr><th>الطريقة</th><th>المحصل</th><th>المسترد</th><th>الصافي</th></tr></thead><tbody className={tableStyles.body}>{summary.paymentTotalsByMethod.map((row) => <tr key={`${row.paymentMethodId}-${row.code}`}><td><strong>{row.displayNameArabic}</strong><small className="mt-1 block font-mono text-stone-400" dir="ltr">{row.code}</small></td><td>{formatEgp(row.receivedAmount)}</td><td>{formatEgp(row.refundedAmount)}</td><td className="font-black">{formatEgp(row.netAmount)}</td></tr>)}</tbody></DataTableWrapper>}
        </Card>
        <Card className="p-5">
          <h2 className="mb-4 text-lg font-black text-stone-950">المصروفات حسب التصنيف</h2>
          {summary.expenseTotalsByCategory.length === 0 ? <EmptyState title="لا توجد مصروفات" /> : <DataTableWrapper label="تصنيفات المصروفات"><thead className={tableStyles.head}><tr><th>التصنيف</th><th>العدد</th><th>الإجمالي</th></tr></thead><tbody className={tableStyles.body}>{summary.expenseTotalsByCategory.map((row) => <tr key={`${row.categoryId}-${row.nameArabic}`}><td className="font-bold">{row.nameArabic}</td><td>{formatNumber(row.expenseCount)}</td><td className="font-black">{formatEgp(row.amount)}</td></tr>)}</tbody></DataTableWrapper>}
        </Card>
      </section>

      <section className="grid gap-5 xl:grid-cols-2">
        <Card className="p-5">
          <h2 className="mb-4 text-lg font-black text-stone-950">أفضل الخدمات والمنتجات</h2>
          {summary.topServicesAndProducts.length === 0 ? <EmptyState title="لا توجد بنود مباعة" /> : <DataTableWrapper label="أفضل البنود"><thead className={tableStyles.head}><tr><th>البند</th><th>النوع</th><th>الكمية</th><th>صافي الإيراد</th></tr></thead><tbody className={tableStyles.body}>{summary.topServicesAndProducts.map((row) => <tr key={`${row.catalogItemId}-${row.itemCode}`}><td><strong>{row.itemNameArabic}</strong><small className="mt-1 block font-mono text-stone-400" dir="ltr">{row.itemCode}</small></td><td>{catalogItemTypeLabels[row.itemType]}</td><td><bdi dir="ltr">{formatQuantity(row.quantity)}</bdi></td><td className="font-black">{formatEgp(row.netRevenue)}</td></tr>)}</tbody></DataTableWrapper>}
        </Card>
        <Card className="p-5">
          <h2 className="mb-4 text-lg font-black text-stone-950">أفضل الموظفين أداءً</h2>
          {summary.topPerformingEmployees.length === 0 ? <EmptyState title="لا توجد خدمات مسندة" /> : <DataTableWrapper label="أداء الموظفين"><thead className={tableStyles.head}><tr><th>الموظف</th><th>الفواتير</th><th>الكمية</th><th>صافي الإيراد</th></tr></thead><tbody className={tableStyles.body}>{summary.topPerformingEmployees.map((row) => <tr key={`${row.employeeId}-${row.employeeName}`}><td className="font-bold">{row.employeeName}</td><td>{formatNumber(row.invoiceCount)}</td><td><bdi dir="ltr">{formatQuantity(row.quantity)}</bdi></td><td className="font-black">{formatEgp(row.netRevenue)}</td></tr>)}</tbody></DataTableWrapper>}
        </Card>
      </section>

      <Card className="p-5"><h2 className="text-lg font-black text-stone-950">اتجاه صافي المبيعات اليومي</h2><p className="mt-1 text-xs text-stone-500">الأيام بلا نشاط لا ينشئها الخادم كسجلات اتجاه.</p><div className="mt-4"><DailySalesChart rows={summary.dailySalesTrend} /></div></Card>
    </div>
  );
}
