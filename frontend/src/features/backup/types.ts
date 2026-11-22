export type BackupRunStatus = "DISABLED" | "NEVER_RUN" | "RUNNING" | "SUCCESS" | "FAILED";

export interface BackupStatus {
  enabled: boolean;
  inProgress: boolean;
  pgDumpAvailable: boolean;
  pgRestoreAvailable: boolean;
  lastAttemptStatus: BackupRunStatus;
  lastAttemptAt: string | null;
  lastSuccessfulAt: string | null;
  lastFileName: string | null;
  lastFileSizeBytes: number | null;
  lastFileSha256: string | null;
  backupDirectory: string;
  retentionCount: number;
  message: string;
}

export interface BackupConfiguration {
  backupDirectory: string;
  automaticEnabled: boolean;
  automaticTime: string;
  retentionCount: number;
}

export interface BackupHistoryItem {
  id: number;
  fileName: string;
  backupType: "MANUAL" | "AUTOMATIC" | "SAFETY";
  status: BackupRunStatus;
  fileSizeBytes: number | null;
  sha256: string | null;
  startedAt: string;
  completedAt: string | null;
  errorMessage: string | null;
}
