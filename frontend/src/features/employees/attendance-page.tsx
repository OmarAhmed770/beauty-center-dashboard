import { CalendarCheck, CircleCheck, CircleX } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { employeeKeys, getEmployeeAttendance, saveEmployeeAttendance } from "@/features/employees/api";
import type { EmployeeAttendance, EmployeeAttendanceStatus } from "@/features/employees/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { invalidateFinancialViews } from "@/lib/financial-query-invalidation";
import { formatMoney } from "@/lib/format";
import { cairoDateIso } from "@/components/shared/date-range-filter";

export function EmployeeAttendancePage() {
  const [businessDate, setBusinessDate] = useState(cairoDateIso);
  const queryClient = useQueryClient();
  const attendanceQuery = useQuery({
    queryKey: employeeKeys.attendance(businessDate),
    queryFn: () => getEmployeeAttendance(businessDate),
  });
  const attendanceMutation = useMutation({
    mutationFn: ({ employeeId, status }: { employeeId: number; status: EmployeeAttendanceStatus }) =>
      saveEmployeeAttendance(employeeId, businessDate, status),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: employeeKeys.all }),
        invalidateFinancialViews(queryClient),
      ]);
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="حضور الموظفين"
        description="سجّل حضور اليوم؛ اختيار حاضر يضيف الأجر اليومي كمستحق فقط، وليس كدفعة نقدية."
      />

      <Card className="p-4 sm:p-5">
        <label className="block max-w-xs text-sm font-black text-ink" htmlFor="attendance-date">
          تاريخ الحضور
          <Input id="attendance-date" className="mt-2" type="date" value={businessDate} onChange={(event) => setBusinessDate(event.target.value)} />
        </label>
      </Card>

      {attendanceQuery.isPending ? <LoadingState label="جارٍ تحميل الحضور..." /> : attendanceQuery.isError ? <ErrorState error={attendanceQuery.error} onRetry={() => void attendanceQuery.refetch()} /> : attendanceQuery.data.length === 0 ? <Card className="p-6 text-center text-sm font-bold text-muted">لا يوجد موظفون نشطون لهذا التاريخ.</Card> : (
        <section className="space-y-3" aria-label="سجل حضور الموظفين">
          {attendanceQuery.data.map((attendance) => (
            <AttendanceRow
              key={attendance.employeeId}
              attendance={attendance}
              busy={attendanceMutation.isPending && attendanceMutation.variables?.employeeId === attendance.employeeId}
              onChoose={(status) => attendanceMutation.mutate({ employeeId: attendance.employeeId, status })}
            />
          ))}
        </section>
      )}

      {attendanceMutation.isError && <div className="rounded-xl border border-danger/25 bg-danger/10 px-4 py-3 text-sm font-bold text-danger" role="alert">{getArabicApiErrorMessage(attendanceMutation.error)}</div>}
    </div>
  );
}

function AttendanceRow({ attendance, busy, onChoose }: { attendance: EmployeeAttendance; busy: boolean; onChoose: (status: EmployeeAttendanceStatus) => void }) {
  const statusText = attendance.status === "PRESENT" ? "حاضر" : attendance.status === "ABSENT" ? "غائب" : "لم يُسجل بعد";
  return (
    <Card className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
      <div className="min-w-0">
        <p className="truncate text-base font-black text-ink">{attendance.employeeName}</p>
        <p className="mt-1 text-sm font-bold text-muted"><bdi dir="ltr">{formatMoney(attendance.dailyWageAmount)}</bdi> أجر يومي · {statusText}</p>
        {attendance.status === "PRESENT" && <p className="mt-1 text-xs font-bold text-success">تم تسجيل استحقاق الأجر اليومي.</p>}
      </div>
      <div className="grid grid-cols-2 gap-2 sm:w-auto">
        <Button type="button" size="lg" variant={attendance.status === "PRESENT" ? "default" : "outline"} disabled={busy} aria-pressed={attendance.status === "PRESENT"} onClick={() => onChoose("PRESENT")}>
          <CircleCheck className="h-5 w-5" />{busy ? "جارٍ الحفظ..." : "حاضر"}
        </Button>
        <Button type="button" size="lg" variant={attendance.status === "ABSENT" ? "secondary" : "outline"} disabled={busy} aria-pressed={attendance.status === "ABSENT"} onClick={() => onChoose("ABSENT")}>
          <CircleX className="h-5 w-5" />غائب
        </Button>
      </div>
    </Card>
  );
}
