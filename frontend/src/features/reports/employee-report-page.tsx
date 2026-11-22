import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { DateRangeFilter, rangeForPreset, type DateRangeValue } from "@/components/shared/date-range-filter";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PrintableViewWrapper } from "@/components/shared/printable-view-wrapper";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { employeeKeys, searchEmployees } from "@/features/employees/api";
import { getEmployeeStatementReport, reportKeys } from "@/features/reports/api";
import { EmployeeStatementView } from "@/features/reports/employee-statement-view";
import { ReportPageHeader } from "@/features/reports/report-page-header";

export function EmployeeReportPage() {
  const [employeeId, setEmployeeId] = useState(0);
  const [range, setRange] = useState<DateRangeValue>(() => rangeForPreset("CURRENT_MONTH"));
  const employees = useQuery({ queryKey: employeeKeys.list({ size: 100 }), queryFn: () => searchEmployees({ page: 0, size: 100 }) });
  const params = { fromDate: range.from, toDate: range.to, page: 0, size: 500 };
  const report = useQuery({ queryKey: reportKeys.detail("employee", { employeeId, ...params }), queryFn: () => getEmployeeStatementReport(employeeId, params), enabled: employeeId > 0 });
  return <div className="space-y-6"><ReportPageHeader title="كشف حساب موظف" description="يشمل الحركات والإجماليات النشطة وصافي الاستحقاق وفق الصيغة المعتمدة حاليًا." /><Card className="space-y-4 p-5"><label className="block max-w-xl text-sm font-bold">الموظف<Select className="mt-2" value={employeeId || ""} onChange={(event) => setEmployeeId(Number(event.target.value))}><option value="">اختر الموظف</option>{employees.data?.content.map((employee) => <option key={employee.id} value={employee.id}>{employee.name} — {employee.position}</option>)}</Select></label><DateRangeFilter value={range} onChange={setRange} /></Card>{employees.isError ? <ErrorState error={employees.error} onRetry={() => void employees.refetch()} /> : employeeId === 0 ? <EmptyState title="اختر موظفًا" description="حدد الموظف والفترة لعرض كشف الحساب القابل للطباعة." /> : report.isPending ? <LoadingState /> : report.isError ? <ErrorState error={report.error} onRetry={() => void report.refetch()} /> : <PrintableViewWrapper title="طباعة كشف الموظف"><EmployeeStatementView statement={report.data} />{report.data.transactions.totalElements > report.data.transactions.content.length && <p className="mt-4 text-xs font-bold text-amber-800 print:hidden">يعرض التقرير أول 500 حركة. ضيّق الفترة لطباعة بقية السجلات.</p>}</PrintableViewWrapper>}</div>;
}
