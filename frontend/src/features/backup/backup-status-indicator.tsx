import { useQuery } from "@tanstack/react-query";
import { DatabaseBackup } from "lucide-react";
import { backupKeys, getBackupStatus } from "@/features/backup/api";
import { useAuth } from "@/features/auth/auth-context";
import { formatArabicDateTime } from "@/lib/format";

export function BackupStatusIndicator({ compact = false }: { compact?: boolean }) {
  const { hasPermission } = useAuth();
  const allowed = hasPermission("BACKUP_MANAGEMENT");
  const query = useQuery({ queryKey: backupKeys.status, queryFn: getBackupStatus, enabled: allowed, staleTime: 60_000, refetchInterval: 5 * 60_000 });
  if (!allowed) return <span className="flex items-center gap-2"><DatabaseBackup className="h-4 w-4" />حالة النسخ: للمدير</span>;
  const label = query.isPending ? "جارٍ التحقق" : query.isError ? "تعذر التحقق" : query.data.lastSuccessfulAt ? formatArabicDateTime(query.data.lastSuccessfulAt) : query.data.enabled ? "لم يُنفذ بعد" : "غير مفعّل";
  return <span className="flex items-center gap-2" title={query.data?.message}><DatabaseBackup className={`h-4 w-4 ${query.data?.lastAttemptStatus === "FAILED" || query.isError ? "text-red-500" : query.data?.lastAttemptStatus === "SUCCESS" ? "text-emerald-500" : "text-stone-400"}`} /><span>{compact ? label : `آخر نسخة: ${label}`}</span></span>;
}
