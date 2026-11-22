import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { DialogShell } from "@/components/shared/dialog-shell";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { MoneyInput } from "@/components/shared/money-input";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { createEmployee, employeeKeys, getEmployeePermissions, updateEmployee, updateEmployeePermissions } from "@/features/employees/api";
import { useAuth } from "@/features/auth/auth-context";
import { employeeSchema, type EmployeeFormValues } from "@/features/employees/model";
import type { Employee } from "@/features/employees/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import type { SystemPermission } from "@/types/auth";

const permissionLabels: Array<{ value: SystemPermission; label: string }> = [
  { value: "DASHBOARD", label: "الرئيسية" },
  { value: "ADD_ORDERS", label: "إضافة أوردرات" },
  { value: "EXPENSES", label: "المصروفات" },
  { value: "SERVICES_AND_PRICES", label: "الخدمات والأسعار" },
  { value: "EMPLOYEES", label: "الموظفون" },
  { value: "DAILY_CLOSING", label: "قفل اليوم" },
  { value: "REPORTS", label: "التقارير" },
  { value: "INVOICES_AND_RETURNS", label: "الفواتير والمرتجعات" },
  { value: "SETTINGS", label: "الإعدادات" },
  { value: "BACKUP_MANAGEMENT", label: "النسخ الاحتياطي" },
];

export function EmployeeFormDialog({ open, employee, onOpenChange, onSaved }: { open: boolean; employee?: Employee | null; onOpenChange: (open: boolean) => void; onSaved?: (employee: Employee) => void }) {
  const queryClient = useQueryClient();
  const { hasAnyRole } = useAuth();
  const owner = hasAnyRole(["OWNER"]);
  const permissions = useQuery({ queryKey: employee ? employeeKeys.permissions(employee.id) : ["employees", "new", "permissions"], queryFn: () => getEmployeePermissions(employee!.id), enabled: Boolean(open && employee && owner) });
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<EmployeeFormValues>({
    resolver: zodResolver(employeeSchema),
    defaultValues: { name: "", phone: "", position: "", defaultDailyWage: "0.00", notes: "" },
  });

  useEffect(() => {
    if (!open) return;
    reset(employee ? {
      name: employee.name,
      phone: employee.phone ?? "",
      position: employee.position,
      defaultDailyWage: String(employee.defaultDailyWage),
      notes: employee.notes ?? "",
    } : { name: "", phone: "", position: "", defaultDailyWage: "0.00", notes: "" });
  }, [employee, open, reset]);

  const mutation = useMutation({
    mutationFn: (values: EmployeeFormValues) => employee ? updateEmployee(employee.id, values) : createEmployee(values),
    onSuccess: async (saved) => {
      await queryClient.invalidateQueries({ queryKey: employeeKeys.all });
      queryClient.setQueryData(employeeKeys.detail(saved.id), saved);
      onSaved?.(saved);
      onOpenChange(false);
    },
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });

  const submit = handleSubmit((values) => mutation.mutate(values));

  return (
    <DialogShell
      open={open}
      title={employee ? "تعديل بيانات الموظف" : "إضافة موظف جديد"}
      description="ستُراجع البيانات مرة أخرى في الخادم قبل الحفظ."
      onOpenChange={onOpenChange}
      size="lg"
      footer={<><Button variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>رجوع</Button><Button type="submit" form="employee-form" disabled={mutation.isPending}>{mutation.isPending ? "جارٍ الحفظ..." : employee ? "حفظ التعديلات" : "إضافة الموظف"}</Button></>}
    >
      <form id="employee-form" className="grid gap-5 sm:grid-cols-2" onSubmit={submit} noValidate>
        <FormField label="اسم الموظف" htmlFor="employee-name" required error={errors.name?.message}>
          <Input id="employee-name" autoFocus maxLength={150} {...register("name")} />
        </FormField>
        <FormField label="رقم الهاتف" htmlFor="employee-phone" error={errors.phone?.message}>
          <Input id="employee-phone" dir="ltr" inputMode="tel" maxLength={50} {...register("phone")} />
        </FormField>
        <FormField label="المسمى الوظيفي" htmlFor="employee-position" required error={errors.position?.message}>
          <Input id="employee-position" maxLength={100} {...register("position")} />
        </FormField>
        <FormField label="الأجر اليومي الافتراضي" htmlFor="employee-wage" required error={errors.defaultDailyWage?.message}>
          <MoneyInput id="employee-wage" {...register("defaultDailyWage")} />
        </FormField>
        <div className="sm:col-span-2">
          <FormField label="ملاحظات" htmlFor="employee-notes" error={errors.notes?.message}>
            <Textarea id="employee-notes" maxLength={2000} {...register("notes")} />
          </FormField>
        </div>
        {employee && owner && <div className="sm:col-span-2"><EmployeePermissionsPanel employee={employee} permissions={permissions.data} loading={permissions.isPending} /></div>}
        <div className="sm:col-span-2"><FormServerError message={errors.root?.server?.message} /></div>
      </form>
    </DialogShell>
  );
}

function EmployeePermissionsPanel({ employee, permissions, loading }: { employee: Employee; permissions: import("@/features/employees/types").EmployeePermissions | undefined; loading: boolean }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: (next: SystemPermission[]) => updateEmployeePermissions(employee.id, next),
    onSuccess: (saved) => queryClient.setQueryData(employeeKeys.permissions(employee.id), saved),
  });
  if (loading) return <p className="text-sm text-stone-500">جارٍ تحميل صلاحيات الحساب...</p>;
  if (!permissions?.accountLinked) return <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm font-bold text-amber-900">اربط هذا الموظف بحساب مستخدم من صفحة المستخدمين أولًا، ثم حدّد صلاحياته هنا.</div>;
  const toggle = (permission: SystemPermission) => {
    const current = new Set(permissions.permissions);
    current.has(permission) ? current.delete(permission) : current.add(permission);
    mutation.mutate([...current]);
  };
  return <fieldset className="rounded-2xl border border-[#f0dce5] bg-[#fff9fb] p-4"><legend className="px-1 text-sm font-black text-[#3A2530]">صلاحيات الحساب المرتبط</legend><p className="mb-3 text-xs text-[#7A6670]">الحساب: <bdi dir="ltr">{permissions.username}</bdi> — يملك المالك فقط تعديل هذه الصلاحيات.</p><div className="grid gap-2 sm:grid-cols-2">{permissionLabels.map(({ value, label }) => <label key={value} className="flex min-h-11 items-center gap-3 rounded-xl border border-[#f3d4e1] bg-white px-3 text-sm font-bold"><input type="checkbox" checked={permissions.permissions.includes(value)} disabled={mutation.isPending} onChange={() => toggle(value)} className="h-5 w-5 accent-[#C2417A]" />{label}</label>)}</div>{mutation.isError && <p className="mt-3 text-sm font-bold text-red-700">{getArabicApiErrorMessage(mutation.error)}</p>}</fieldset>;
}
