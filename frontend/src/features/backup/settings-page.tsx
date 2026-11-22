import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DatabaseBackup, RotateCcw, Save, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { ConfirmationDialog } from "@/components/shared/confirmation-dialog";
import { DialogShell } from "@/components/shared/dialog-shell";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useAuth } from "@/features/auth/auth-context";
import { backupKeys, deleteBackup, getBackupConfiguration, getBackupHistory, getBackupStatus, restoreBackup, runManualBackup, updateBackupConfiguration } from "@/features/backup/api";
import type { BackupConfiguration, BackupHistoryItem, BackupRunStatus } from "@/features/backup/types";
import { BusinessProfileCard } from "@/features/business/business-profile-card";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import { formatArabicDateTime, formatNumber } from "@/lib/format";

const statusLabels: Record<BackupRunStatus, string> = { DISABLED: "غير مفعّل", NEVER_RUN: "لم يُنفذ", RUNNING: "جارٍ التنفيذ", SUCCESS: "ناجح", FAILED: "فشل" };
const statusTones = { DISABLED: "neutral", NEVER_RUN: "warning", RUNNING: "info", SUCCESS: "success", FAILED: "danger" } as const;

export function SettingsPage() {
  const [searchParams] = useSearchParams();
  const { hasAnyRole } = useAuth();
  if (searchParams.get("section") === "backup") return hasAnyRole(["OWNER"]) ? <BackupSettings /> : <Navigate to="/forbidden" replace />;
  if (searchParams.get("section") === "business") return <BusinessSettings />;
  return <SettingsHub />;
}

function SettingsHub() {
  const { hasAnyRole, hasPermission } = useAuth();
  const owner = hasAnyRole(["OWNER"]);
  const links = [
    ...(hasPermission("SETTINGS") ? [
      { to: "/discounts", label: "الخصومات المجدولة" },
      { to: "/expenses/settings", label: "المصروفات وطرق الدفع" },
    ] : []),
    ...(hasPermission("SERVICES_AND_PRICES") ? [
      { to: "/catalog/hair", label: "تركيب الشعر" },
      { to: "/reports/price-history", label: "سجل الأسعار" },
      { to: "/catalog/categories", label: "تصنيفات الخدمات" },
    ] : []),
    ...(owner ? [{ to: "/users", label: "المستخدمين والموافقات" }] : []),
    ...(owner ? [{ to: "/settings?section=backup", label: "النسخ الاحتياطي" }] : []),
    ...(owner ? [{ to: "/settings?section=business", label: "إعدادات المركز" }] : []),
  ];
  return <section className="max-w-2xl divide-y divide-[#eee7ea] border-y border-[#eee7ea]">{links.map((link) => <Link key={link.to} to={link.to} className="flex min-h-14 items-center justify-between px-1 text-sm font-black text-[#29252A] transition-colors hover:bg-[#f8f8f8] sm:px-3"><span>{link.label}</span><span className="text-[#A83868]" aria-hidden="true">‹</span></Link>)}</section>;
}

function BusinessSettings() {
  const { hasAnyRole } = useAuth();
  return <div className="space-y-5"><BackToSettings /><BusinessProfileCard editable={hasAnyRole(["OWNER"])} /></div>;
}

function BackupSettings() {
  const queryClient = useQueryClient();
  const { hasAnyRole } = useAuth();
  const owner = hasAnyRole(["OWNER"]);
  const [confirm, setConfirm] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [restoreTarget, setRestoreTarget] = useState<BackupHistoryItem | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<BackupHistoryItem | null>(null);
  const query = useQuery({ queryKey: backupKeys.status, queryFn: getBackupStatus });
  const configuration = useQuery({ queryKey: backupKeys.configuration, queryFn: getBackupConfiguration });
  const history = useQuery({ queryKey: backupKeys.history, queryFn: getBackupHistory });
  const refresh = async () => Promise.all([queryClient.invalidateQueries({ queryKey: backupKeys.status }), queryClient.invalidateQueries({ queryKey: backupKeys.configuration }), queryClient.invalidateQueries({ queryKey: backupKeys.history })]);
  const mutation = useMutation({
    mutationFn: runManualBackup,
    onSuccess: (data) => { queryClient.setQueryData(backupKeys.status, data); void refresh(); setConfirm(false); setMessage(data.message); },
    onError: (error) => { setConfirm(false); setMessage(getArabicApiErrorMessage(error)); },
  });
  return <div className="space-y-7"><BackToSettings />{query.isPending ? <LoadingState /> : query.isError ? <ErrorState error={query.error} onRetry={() => void query.refetch()} /> : <>
    <Card className="p-5"><div className="flex flex-wrap items-center justify-between gap-4"><div className="flex items-center gap-3"><span className="grid h-12 w-12 place-items-center rounded-xl bg-[#FDF1F6] text-[#C2417A]"><DatabaseBackup /></span><div><h2 className="font-black">النسخ الاحتياطي المحلي</h2><p className="mt-1 text-sm text-stone-500">{query.data.message}</p></div></div><StatusBadge tone={statusTones[query.data.lastAttemptStatus]}>{statusLabels[query.data.lastAttemptStatus]}</StatusBadge></div><dl className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><Info label="آخر نسخة ناجحة" value={formatArabicDateTime(query.data.lastSuccessfulAt)} /><Info label="اسم الملف" value={query.data.lastFileName || "—"} ltr /><Info label="حد الاحتفاظ" value={`${formatNumber(query.data.retentionCount)} نسخة`} /><Info label="الحجم" value={query.data.lastFileSizeBytes ? `${(query.data.lastFileSizeBytes / 1024 / 1024).toFixed(2)} MB` : "—"} /></dl><div className="mt-5 flex flex-wrap gap-2"><StatusBadge tone={query.data.pgDumpAvailable ? "success" : "danger"}>pg_dump {query.data.pgDumpAvailable ? "جاهز" : "غير موجود"}</StatusBadge><StatusBadge tone={query.data.pgRestoreAvailable ? "success" : "danger"}>pg_restore {query.data.pgRestoreAvailable ? "جاهز" : "غير موجود"}</StatusBadge></div><div className="mt-5 flex flex-wrap gap-3"><Button size="lg" disabled={!query.data.enabled || !query.data.pgDumpAvailable || mutation.isPending || query.data.inProgress} onClick={() => { setMessage(null); setConfirm(true); }}><DatabaseBackup className="h-5 w-5" />إنشاء نسخة احتياطية الآن</Button></div>{message && <p className={`mt-4 text-sm font-bold ${mutation.isError ? "text-red-700" : "text-emerald-700"}`} role="status">{message}</p>}</Card>
    {configuration.data && <BackupConfigurationCard value={configuration.data} onSaved={refresh} />}
    <BackupHistory history={history.data ?? []} loading={history.isPending} canRestore={owner} onRestore={setRestoreTarget} onDelete={setDeleteTarget} />
    <ConfirmationDialog open={confirm} title="إنشاء نسخة احتياطية الآن" description="قد تستغرق العملية عدة دقائق. ستُحفظ نسخة PostgreSQL مخصصة في المجلد الخارجي وتُطبق سياسة الاحتفاظ بعد نجاحها." confirmLabel="بدء النسخ" busy={mutation.isPending} onOpenChange={setConfirm} onConfirm={() => mutation.mutate()} />
    <ConfirmationDialog open={Boolean(deleteTarget)} title="حذف النسخة الاحتياطية" description={`سيُحذف الملف ${deleteTarget?.fileName ?? ""} وسجله. لا يمكن التراجع عن ذلك.`} confirmLabel="حذف النسخة" destructive onOpenChange={(open) => !open && setDeleteTarget(null)} onConfirm={() => deleteTarget && deleteBackup(deleteTarget.id).then(() => { setDeleteTarget(null); return refresh(); }).catch((error) => setMessage(getArabicApiErrorMessage(error)))} />
    <RestoreDialog backup={restoreTarget} onClose={() => setRestoreTarget(null)} onRestored={async () => { setRestoreTarget(null); await refresh(); }} />
  </>}</div>;
}

function BackupConfigurationCard({ value, onSaved }: { value: BackupConfiguration; onSaved: () => Promise<unknown> }) {
  const [draft, setDraft] = useState(value);
  useEffect(() => setDraft(value), [value]);
  const mutation = useMutation({ mutationFn: () => updateBackupConfiguration(draft), onSuccess: onSaved });
  return <Card className="p-5"><h2 className="font-black">إعداد النسخ المحلي</h2><div className="mt-4 grid gap-4 sm:grid-cols-2"><label className="sm:col-span-2 text-sm font-bold">مجلد النسخ<input className="mt-2 w-full rounded-xl border border-[#F3D4E1] bg-white px-3 py-2.5 font-mono text-sm" dir="ltr" value={draft.backupDirectory} onChange={(event) => setDraft({ ...draft, backupDirectory: event.target.value })} /></label><label className="text-sm font-bold">وقت النسخ اليومي<input className="mt-2 w-full rounded-xl border border-[#F3D4E1] bg-white px-3 py-2.5" type="time" value={draft.automaticTime} onChange={(event) => setDraft({ ...draft, automaticTime: event.target.value })} /></label><label className="text-sm font-bold">عدد النسخ المحفوظة<input className="mt-2 w-full rounded-xl border border-[#F3D4E1] bg-white px-3 py-2.5" type="number" min="1" max="365" value={draft.retentionCount} onChange={(event) => setDraft({ ...draft, retentionCount: Number(event.target.value) })} /></label><label className="flex min-h-11 items-center gap-3 text-sm font-bold sm:col-span-2"><input type="checkbox" checked={draft.automaticEnabled} onChange={(event) => setDraft({ ...draft, automaticEnabled: event.target.checked })} className="h-5 w-5 accent-[#C2417A]" />تفعيل النسخ اليومي التلقائي</label></div><Button className="mt-5" variant="outline" disabled={mutation.isPending || !draft.backupDirectory.trim()} onClick={() => mutation.mutate()}><Save className="h-4 w-4" />حفظ الإعدادات</Button>{mutation.isError && <p className="mt-3 text-sm font-bold text-red-700">{getArabicApiErrorMessage(mutation.error)}</p>}</Card>;
}

function BackupHistory({ history, loading, canRestore, onRestore, onDelete }: { history: BackupHistoryItem[]; loading: boolean; canRestore: boolean; onRestore: (item: BackupHistoryItem) => void; onDelete: (item: BackupHistoryItem) => void }) {
  return <Card className="p-5"><h2 className="font-black">سجل النسخ الاحتياطية</h2>{loading ? <LoadingState label="جارٍ تحميل السجل..." /> : history.length === 0 ? <p className="mt-4 text-sm text-stone-500">لا توجد نسخ محفوظة بعد.</p> : <div className="mt-4 space-y-3">{history.map((item) => <div key={item.id} className="rounded-xl border border-[#F3D4E1] p-3"><div className="flex flex-wrap items-center justify-between gap-2"><div><p className="font-mono text-sm font-black" dir="ltr">{item.fileName}</p><p className="mt-1 text-xs text-stone-500">{formatArabicDateTime(item.completedAt ?? item.startedAt)} · {item.fileSizeBytes ? `${(item.fileSizeBytes / 1024 / 1024).toFixed(2)} MB` : "—"}</p></div><StatusBadge tone={statusTones[item.status]}>{statusLabels[item.status]}</StatusBadge></div>{item.status === "SUCCESS" && <div className="mt-3 flex flex-wrap gap-2">{canRestore && <Button size="sm" variant="outline" onClick={() => onRestore(item)}><RotateCcw className="h-4 w-4" />استعادة</Button>}<Button size="sm" variant="ghost" className="text-red-700" onClick={() => onDelete(item)}><Trash2 className="h-4 w-4" />حذف</Button></div>}</div>)}</div>}</Card>;
}

function RestoreDialog({ backup, onClose, onRestored }: { backup: BackupHistoryItem | null; onClose: () => void; onRestored: () => Promise<void> }) {
  const [password, setPassword] = useState(""); const [confirmation, setConfirmation] = useState("");
  useEffect(() => { setPassword(""); setConfirmation(""); }, [backup?.id]);
  const mutation = useMutation({ mutationFn: () => restoreBackup(backup!.id, password, confirmation), onSuccess: onRestored });
  const required = backup ? `استعادة ${backup.fileName}` : "";
  return <DialogShell open={Boolean(backup)} title="استعادة نسخة احتياطية" description="الاستعادة هترجّع بيانات المركز لحالة النسخة المختارة، وأي تغييرات بعدها مش هتظهر. هنعمل نسخة أمان الأول، والحفظ هيتوقف مؤقتًا لحد ما الاستعادة تخلص." onOpenChange={(open) => !open && onClose()} footer={<><Button variant="outline" onClick={onClose}>رجوع</Button><Button variant="destructive" form="restore-backup" disabled={mutation.isPending || confirmation !== required || !password}>{mutation.isPending ? "جارٍ الاستعادة..." : "استعادة الآن"}</Button></>}><form id="restore-backup" className="space-y-4" onSubmit={(event) => { event.preventDefault(); mutation.mutate(); }}><label className="block text-sm font-bold">كلمة مرور المالك<input className="mt-2 w-full rounded-xl border border-[#F3D4E1] px-3 py-2.5" type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label><label className="block text-sm font-bold">اكتب للتأكيد: <bdi dir="ltr">{required}</bdi><input className="mt-2 w-full rounded-xl border border-[#F3D4E1] px-3 py-2.5" dir="rtl" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} /></label>{mutation.isError && <p className="text-sm font-bold text-red-700">{getArabicApiErrorMessage(mutation.error)}</p>}</form></DialogShell>;
}

function BackToSettings() {
  return <Link to="/settings" className="inline-flex min-h-11 items-center text-sm font-black text-[#A83868] hover:underline">العودة إلى الإعدادات</Link>;
}

function Info({ label, value, ltr = false }: { label: string; value: string; ltr?: boolean }) { return <div><dt className="text-xs font-bold text-stone-500">{label}</dt><dd className="mt-1 break-all font-black" dir={ltr ? "ltr" : undefined}>{value}</dd></div>; }
