import { ArrowRight } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { PaginationControls } from "@/components/shared/pagination-controls";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { catalogKeys, getCatalogItem, searchPriceHistory } from "@/features/catalog/api";
import { priceValueTypeLabels } from "@/features/catalog/model";
import type { PriceValueType } from "@/features/catalog/types";
import { formatArabicDateTime, formatEgp } from "@/lib/format";

export function PriceHistoryPage() {
  const itemId = Number(useParams<{ itemId: string }>().itemId);
  const [valueType, setValueType] = useState<PriceValueType | "">("");
  const [page, setPage] = useState(0);
  const itemQuery = useQuery({ queryKey: catalogKeys.item(itemId), queryFn: () => getCatalogItem(itemId), enabled: Number.isInteger(itemId) && itemId > 0 });
  const params = { catalogItemId: itemId, valueType: valueType || undefined, page, size: 20 };
  const historyQuery = useQuery({ queryKey: catalogKeys.priceHistory(params), queryFn: () => searchPriceHistory(params), enabled: itemQuery.isSuccess });
  if (itemQuery.isPending) return <LoadingState label="جارٍ تحميل الصنف..." />;
  if (itemQuery.isError) return <ErrorState error={itemQuery.error} onRetry={() => void itemQuery.refetch()} />;
  return <div className="space-y-6"><PageHeader title={`سجل الأسعار — ${itemQuery.data.nameArabic}`} description="سجل غير قابل للتعديل لكل تغيير في سعر البيع أو سعر الشراء." actions={<Button asChild variant="outline"><Link to="/catalog"><ArrowRight className="h-4 w-4" />الكتالوج</Link></Button>} /><div className="max-w-xs"><Select aria-label="نوع القيمة" value={valueType} onChange={(event) => { setValueType(event.target.value as PriceValueType | ""); setPage(0); }}><option value="">كل التغييرات</option><option value="SELLING_PRICE">سعر البيع</option><option value="COST">سعر الشراء</option></Select></div>{historyQuery.isPending ? <LoadingState label="جارٍ تحميل سجل الأسعار..." /> : historyQuery.isError ? <ErrorState error={historyQuery.error} onRetry={() => void historyQuery.refetch()} /> : historyQuery.data.content.length === 0 ? <EmptyState title="لا توجد تغييرات سعرية" description="قيمة الإنشاء الابتدائية لا تنشئ سجل تغيير." /> : <><DataTableWrapper label="سجل أسعار الصنف"><thead className={tableStyles.head}><tr><th>نوع القيمة</th><th>القيمة القديمة</th><th>القيمة الجديدة</th><th>السبب</th><th>غيّرها</th><th>وقت السريان</th></tr></thead><tbody className={tableStyles.body}>{historyQuery.data.content.map((entry) => <tr key={entry.id}><td className="font-bold">{priceValueTypeLabels[entry.valueType]}</td><td className="tabular-nums">{formatEgp(entry.oldValue)}</td><td className="font-black tabular-nums text-rose-800">{formatEgp(entry.newValue)}</td><td>{entry.changeReason}</td><td>{entry.changedBy}</td><td>{formatArabicDateTime(entry.effectiveAt)}</td></tr>)}</tbody></DataTableWrapper><PaginationControls page={historyQuery.data.page} totalPages={historyQuery.data.totalPages} totalElements={historyQuery.data.totalElements} onPageChange={setPage} /></>}</div>;
}
