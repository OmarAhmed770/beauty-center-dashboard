import { type UseQueryResult, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, KeyRound, Pencil, Plus, Power, UsersRound, XCircle } from "lucide-react";
import { useState } from "react";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { DataTableWrapper, tableStyles } from "@/components/shared/data-table-wrapper";
import { DialogShell } from "@/components/shared/dialog-shell";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { LoadingState } from "@/components/shared/loading-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { employeeKeys, searchEmployees } from "@/features/employees/api";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import type { RegistrationStatus, UserResponse, UserRole } from "@/types/auth";
import { approveRegistration, createUser, getPendingRegistrations, getUsers, rejectRegistration, resetUserPassword, setUserActive, updateUser, userKeys, type EmployeeLinkMode } from "@/features/users/api";

const staffRole: Extract<UserRole, "RECEPTIONIST"> = "RECEPTIONIST";
const roleLabels: Record<UserRole, string> = { OWNER: "مالك", RECEPTIONIST: "موظف" };
const statusLabels: Record<RegistrationStatus, string> = { PENDING: "بانتظار الموافقة", APPROVED: "معتمد", REJECTED: "مرفوض" };
const statusTones: Record<RegistrationStatus, "warning" | "success" | "danger"> = { PENDING: "warning", APPROVED: "success", REJECTED: "danger" };
const employeeLinkLabels: Record<EmployeeLinkMode, string> = { AUTO_CREATE: "إنشاء موظف مرتبط تلقائيًا", EXISTING: "ربط موظف موجود", NONE: "بدون ربط الآن" };

type EditorState = { mode: "create" } | { mode: "edit"; user: UserResponse } | null;

export function UsersPage() {
  const queryClient = useQueryClient();
  const users = useQuery({ queryKey: userKeys.all, queryFn: getUsers });
  const pending = useQuery({ queryKey: userKeys.pending, queryFn: getPendingRegistrations });
  const [editor, setEditor] = useState<EditorState>(null);
  const [activeTarget, setActiveTarget] = useState<UserResponse | null>(null);
  const [passwordTarget, setPasswordTarget] = useState<UserResponse | null>(null);
  const refresh = async () => Promise.all([queryClient.invalidateQueries({ queryKey: userKeys.all }), queryClient.invalidateQueries({ queryKey: userKeys.pending })]);
  const activeMutation = useMutation({ mutationFn: ({ id, active }: { id: number; active: boolean }) => setUserActive(id, active), onSuccess: async () => { await refresh(); setActiveTarget(null); } });
  const managedUsers = users.data?.filter((user) => user.registrationStatus !== "PENDING") ?? [];

  return <div className="space-y-7">
    <PageHeader title="المستخدمون والصلاحيات" description="المالك فقط يراجع طلبات الموظفين ويدير الحسابات. لا يمكن إنشاء أو تغيير حساب مالك إضافي." actions={<Button onClick={() => setEditor({ mode: "create" })}><Plus className="h-4 w-4" />إضافة موظف</Button>} />
    <PendingRegistrationRequests query={pending} onChanged={refresh} />
    {users.isPending ? <LoadingState label="جارٍ تحميل المستخدمين..." /> : users.isError ? <ErrorState error={users.error} onRetry={() => void users.refetch()} /> : managedUsers.length === 0 ? <EmptyState title="لا توجد حسابات معتمدة أو مرفوضة" /> : <DataTableWrapper label="المستخدمون"><thead className={tableStyles.head}><tr><th>المستخدم</th><th>الصلاحية</th><th>الموظف المرتبط</th><th>حالة التسجيل</th><th>الحالة</th><th>آخر تعديل</th><th>الإجراءات</th></tr></thead><tbody className={tableStyles.body}>{managedUsers.map((user) => <tr key={user.id}><td><p className="font-black text-stone-950">{user.displayName}</p><p className="mt-1 font-mono text-xs text-stone-500" dir="ltr">{user.username}</p>{user.phone && <p className="mt-1 text-xs text-stone-400" dir="ltr">{user.phone}</p>}</td><td className="space-x-1 space-x-reverse">{user.roles.map((role) => <StatusBadge key={role} tone={role === "OWNER" ? "warning" : "info"}>{roleLabels[role]}</StatusBadge>)}</td><td>{user.employeeName ?? <span className="text-sm text-stone-400">غير مرتبط</span>}</td><td><StatusBadge tone={statusTones[user.registrationStatus]}>{statusLabels[user.registrationStatus]}</StatusBadge></td><td><StatusBadge tone={user.active ? "success" : "neutral"}>{user.active ? "نشط" : "متوقف"}</StatusBadge></td><td>{new Intl.DateTimeFormat("ar-EG", { dateStyle: "medium" }).format(new Date(user.updatedAt))}</td><td>{user.registrationStatus === "APPROVED" && <div className="flex flex-wrap gap-2"><Button size="sm" variant="outline" onClick={() => setEditor({ mode: "edit", user })}><Pencil className="h-3.5 w-3.5" />تعديل</Button><Button size="sm" variant="outline" onClick={() => setPasswordTarget(user)}><KeyRound className="h-3.5 w-3.5" />كلمة المرور</Button><Button size="sm" variant={user.active ? "ghost" : "secondary"} onClick={() => setActiveTarget(user)}><Power className="h-3.5 w-3.5" />{user.active ? "إيقاف" : "تفعيل"}</Button></div>}</td></tr>)}</tbody></DataTableWrapper>}
    <UserEditorDialog key={editor?.mode === "edit" ? editor.user.id : editor ? "create" : "closed"} state={editor} onClose={() => setEditor(null)} onSaved={refresh} />
    <PasswordDialog key={passwordTarget?.id ?? "closed"} user={passwordTarget} onClose={() => setPasswordTarget(null)} onSaved={refresh} />
    <ConfirmationDialog open={Boolean(activeTarget)} title={activeTarget?.active ? "إيقاف الحساب" : "تفعيل الحساب"} description={activeTarget?.active ? "لن يتمكن المستخدم من تسجيل الدخول حتى يُعاد تفعيل حسابه." : "سيتمكن المستخدم من تسجيل الدخول حسب صلاحياته الحالية."} confirmLabel={activeTarget?.active ? "إيقاف" : "تفعيل"} destructive={Boolean(activeTarget?.active)} busy={activeMutation.isPending} onOpenChange={(open) => !open && setActiveTarget(null)} onConfirm={() => activeTarget && activeMutation.mutate({ id: activeTarget.id, active: !activeTarget.active })} />
  </div>;
}

function EmployeeLinkFields({ mode, employeeId, onModeChange, onEmployeeChange }: { mode: EmployeeLinkMode; employeeId: string; onModeChange: (mode: EmployeeLinkMode) => void; onEmployeeChange: (employeeId: string) => void }) {
  const employees = useQuery({ queryKey: [...employeeKeys.all, "user-link"], queryFn: () => searchEmployees({ active: true, page: 0, size: 100 }), staleTime: 5 * 60_000 });
  return <fieldset className="space-y-3"><legend className="text-sm font-bold text-stone-700">ربط حساب الموظف</legend><FormField label="اختيار الربط" htmlFor="employee-link-mode"><Select id="employee-link-mode" value={mode} onChange={(event) => { const next = event.target.value as EmployeeLinkMode; onModeChange(next); if (next !== "EXISTING") onEmployeeChange(""); }}>{(Object.keys(employeeLinkLabels) as EmployeeLinkMode[]).map((value) => <option key={value} value={value}>{employeeLinkLabels[value]}</option>)}</Select></FormField>{mode === "EXISTING" && <FormField label="الموظف الحالي" htmlFor="linked-employee" required><Select id="linked-employee" value={employeeId} onChange={(event) => onEmployeeChange(event.target.value)} disabled={employees.isPending || employees.isError}><option value="">اختر الموظف</option>{employees.data?.content.map((employee) => <option key={employee.id} value={employee.id}>{employee.name}</option>)}</Select>{employees.isError && <p className="mt-2 text-sm font-bold text-red-700">تعذر تحميل الموظفين. جرّب مرة أخرى.</p>}</FormField>}</fieldset>;
}

function PendingRegistrationRequests({ query, onChanged }: { query: UseQueryResult<UserResponse[], Error>; onChanged: () => Promise<unknown> }) {
  const [approvalTarget, setApprovalTarget] = useState<UserResponse | null>(null);
  const [rejectTarget, setRejectTarget] = useState<UserResponse | null>(null);
  const approveMutation = useMutation({ mutationFn: ({ id, employeeLinkMode, employeeId }: { id: number; employeeLinkMode: EmployeeLinkMode; employeeId?: number }) => approveRegistration(id, { role: staffRole, employeeLinkMode, employeeId }), onSuccess: async () => { await onChanged(); setApprovalTarget(null); } });
  const rejectMutation = useMutation({ mutationFn: rejectRegistration, onSuccess: async () => { await onChanged(); setRejectTarget(null); } });
  if (query.isPending) return <LoadingState label="جارٍ تحميل طلبات التسجيل..." />;
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />;
  return <section className="space-y-4"><div className="flex flex-wrap items-center gap-3"><span className="grid h-11 w-11 place-items-center rounded-2xl bg-[#FCEAF2] text-[#A83868]"><UsersRound className="h-5 w-5" /></span><div><h2 className="font-black text-[#29252A]">طلبات تسجيل الموظفين</h2><p className="mt-1 text-sm text-[#736873]">{query.data.length ? `${query.data.length.toLocaleString("ar-EG")} طلب بانتظار مراجعتك.` : "لا توجد طلبات معلقة."}</p></div></div>{query.data.length > 0 && <div className="grid gap-3 lg:grid-cols-2">{query.data.map((user) => <Card key={user.id} className="p-5"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="font-black text-[#29252A]">{user.displayName}</p><p className="mt-1 font-mono text-xs text-[#736873]" dir="ltr">{user.username}</p>{user.phone && <p className="mt-1 text-xs text-[#736873]" dir="ltr">{user.phone}</p>}</div><StatusBadge tone="warning">بانتظار الموافقة</StatusBadge></div><p className="mt-4 text-xs text-[#736873]">طُلب في {new Intl.DateTimeFormat("ar-EG", { dateStyle: "medium" }).format(new Date(user.createdAt))}</p><div className="mt-4 flex flex-wrap gap-2 border-t border-[#f1ebee] pt-4"><Button size="sm" onClick={() => setApprovalTarget(user)}><CheckCircle2 className="h-4 w-4" />اعتماد كموظف</Button><Button size="sm" variant="ghost" className="text-red-700" onClick={() => setRejectTarget(user)}><XCircle className="h-4 w-4" />رفض</Button></div></Card>)}</div>}<ApprovalDialog user={approvalTarget} busy={approveMutation.isPending} error={approveMutation.isError ? getArabicApiErrorMessage(approveMutation.error) : undefined} onClose={() => setApprovalTarget(null)} onApprove={(input) => approvalTarget && approveMutation.mutate({ id: approvalTarget.id, ...input })} /><ConfirmationDialog open={Boolean(rejectTarget)} title="رفض طلب التسجيل" description={`لن يتمكن ${rejectTarget?.displayName ?? "الموظف"} من تسجيل الدخول، وسيبقى سجل الطلب محفوظًا.`} confirmLabel="رفض الطلب" destructive busy={rejectMutation.isPending} onOpenChange={(open) => !open && setRejectTarget(null)} onConfirm={() => rejectTarget && rejectMutation.mutate(rejectTarget.id)} /></section>;
}

function ApprovalDialog({ user, busy, error, onClose, onApprove }: { user: UserResponse | null; busy: boolean; error?: string; onClose: () => void; onApprove: (input: { employeeLinkMode: EmployeeLinkMode; employeeId?: number }) => void }) {
  const [employeeLinkMode, setEmployeeLinkMode] = useState<EmployeeLinkMode>("AUTO_CREATE");
  const [employeeId, setEmployeeId] = useState("");
  const valid = employeeLinkMode !== "EXISTING" || Boolean(employeeId);
  return <DialogShell open={Boolean(user)} title="الموافقة على طلب الموظف" description={user ? `سيُعتمد ${user.displayName} بصفة موظف. لا يمكن منح دور المالك أو المدير من هذه العملية.` : undefined} onOpenChange={(open) => !open && onClose()} footer={<><Button variant="outline" onClick={onClose} disabled={busy}>رجوع</Button><Button form="approve-registration" type="submit" disabled={busy || !valid}>{busy ? "جارٍ الاعتماد..." : "اعتماد الحساب"}</Button></>}><form id="approve-registration" className="space-y-5" onSubmit={(event) => { event.preventDefault(); if (valid) onApprove({ employeeLinkMode, employeeId: employeeLinkMode === "EXISTING" ? Number(employeeId) : undefined }); }}><div className="rounded-xl bg-[#FCEAF2] p-3 text-sm font-bold text-[#A83868]">الصلاحية: موظف</div><EmployeeLinkFields mode={employeeLinkMode} employeeId={employeeId} onModeChange={setEmployeeLinkMode} onEmployeeChange={setEmployeeId} /><FormServerError message={error} /></form></DialogShell>;
}

function UserEditorDialog({ state, onClose, onSaved }: { state: EditorState; onClose: () => void; onSaved: () => Promise<unknown> }) {
  const existing = state?.mode === "edit" ? state.user : null;
  const isOwner = existing?.roles.includes("OWNER") ?? false;
  const [displayName, setDisplayName] = useState(existing?.displayName ?? "");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [employeeLinkMode, setEmployeeLinkMode] = useState<EmployeeLinkMode>("AUTO_CREATE");
  const [employeeId, setEmployeeId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const mutation = useMutation({ mutationFn: async () => existing ? updateUser(existing.id, { displayName: displayName.trim(), roles: existing.roles, permissions: existing.permissions }) : createUser({ displayName: displayName.trim(), username: username.trim(), password, roles: [staffRole], employeeLinkMode, employeeId: employeeLinkMode === "EXISTING" ? Number(employeeId) : undefined }), onSuccess: async () => { await onSaved(); onClose(); }, onError: (cause) => setError(getArabicApiErrorMessage(cause)) });
  const valid = displayName.trim().length > 0 && (existing || (username.trim().length >= 3 && password.length >= 12 && (employeeLinkMode !== "EXISTING" || Boolean(employeeId))));
  const roleText = existing?.roles.map((role) => roleLabels[role]).join("، ") ?? "موظف";
  return <DialogShell open={Boolean(state)} title={existing ? "تعديل المستخدم" : "إضافة موظف"} description={isOwner ? "دور المالك الوحيد ثابت؛ يمكن تعديل اسم العرض فقط." : existing ? "اسم المستخدم والصلاحية ثابتان لحماية الحسابات الحالية." : "أنشئ حساب موظف معتمدًا مباشرة."} onOpenChange={(open) => !open && onClose()} footer={<><Button variant="outline" onClick={onClose} disabled={mutation.isPending}>رجوع</Button><Button form="user-editor" type="submit" disabled={mutation.isPending || !valid}>{mutation.isPending ? "جارٍ الحفظ..." : "حفظ المستخدم"}</Button></>}><form id="user-editor" className="grid gap-5 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); setError(null); mutation.mutate(); }}><FormField label="اسم العرض" htmlFor="user-display-name" required><Input id="user-display-name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} /></FormField>{existing ? <FormField label="اسم المستخدم" htmlFor="user-username"><Input id="user-username" value={existing.username} disabled dir="ltr" /></FormField> : <><FormField label="اسم المستخدم" htmlFor="user-username" required hint="أحرف صغيرة وأرقام و . أو _ أو -"><Input id="user-username" value={username} onChange={(event) => setUsername(event.target.value.toLowerCase())} dir="ltr" autoComplete="username" /></FormField><FormField label="كلمة المرور" htmlFor="user-password" required hint="12 حرفًا على الأقل"><Input id="user-password" value={password} onChange={(event) => setPassword(event.target.value)} type="password" autoComplete="new-password" /></FormField></>}<div className="sm:col-span-2"><div className="rounded-2xl bg-[#FCEAF2] px-4 py-3 text-sm font-bold text-[#A83868]">الصلاحية: {roleText}</div></div>{!existing && <div className="sm:col-span-2"><EmployeeLinkFields mode={employeeLinkMode} employeeId={employeeId} onModeChange={setEmployeeLinkMode} onEmployeeChange={setEmployeeId} /></div>}<div className="sm:col-span-2"><FormServerError message={error ?? undefined} /></div></form></DialogShell>;
}

function PasswordDialog({ user, onClose, onSaved }: { user: UserResponse | null; onClose: () => void; onSaved: () => Promise<unknown> }) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const mutation = useMutation({ mutationFn: () => resetUserPassword(user!.id, password), onSuccess: async () => { await onSaved(); onClose(); }, onError: (cause) => setError(getArabicApiErrorMessage(cause)) });
  return <DialogShell open={Boolean(user)} title="إعادة تعيين كلمة المرور" description={user ? `عيّن كلمة مرور جديدة لحساب ${user.displayName}.` : undefined} onOpenChange={(open) => !open && onClose()} footer={<><Button variant="outline" onClick={onClose} disabled={mutation.isPending}>رجوع</Button><Button form="password-reset" type="submit" disabled={mutation.isPending || password.length < 12}>{mutation.isPending ? "جارٍ الحفظ..." : "حفظ كلمة المرور"}</Button></>}><form id="password-reset" className="space-y-5" onSubmit={(event) => { event.preventDefault(); setError(null); mutation.mutate(); }}><FormField label="كلمة المرور الجديدة" htmlFor="reset-password" required hint="12 حرفًا على الأقل"><Input id="reset-password" type="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} /></FormField><FormServerError message={error ?? undefined} /></form></DialogShell>;
}
