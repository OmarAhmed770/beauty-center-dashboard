// TEST ONLY - credential literals in this file are synthetic test fixtures.
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SettingsPage } from "@/features/backup/settings-page";
import type { BackupConfiguration, BackupHistoryItem, BackupStatus } from "@/features/backup/types";

const mocks = vi.hoisted(() => ({
  hasAnyRole: vi.fn(),
  getBackupStatus: vi.fn(),
  getBackupConfiguration: vi.fn(),
  getBackupHistory: vi.fn(),
  runManualBackup: vi.fn(),
  updateBackupConfiguration: vi.fn(),
  deleteBackup: vi.fn(),
  openBackupFolder: vi.fn(),
  restoreBackup: vi.fn(),
}));

vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => ({ hasAnyRole: mocks.hasAnyRole }) }));
vi.mock("@/features/backup/api", () => ({
  ...mocks,
  backupKeys: { status: ["backups", "status"], configuration: ["backups", "configuration"], history: ["backups", "history"] },
}));

const configuration: BackupConfiguration = {
  backupDirectory: "C:\\TestBackups",
  automaticEnabled: true,
  automaticTime: "22:00",
  retentionCount: 14,
};
const backup: BackupHistoryItem = {
  id: 7,
  fileName: "test-backup.dump",
  backupType: "MANUAL",
  status: "SUCCESS",
  fileSizeBytes: 1024,
  sha256: null,
  startedAt: "2026-11-15T10:00:00Z",
  completedAt: "2026-11-15T10:01:00Z",
  errorMessage: null,
};
const status: BackupStatus = {
  enabled: true,
  inProgress: false,
  pgDumpAvailable: true,
  pgRestoreAvailable: true,
  lastAttemptStatus: "SUCCESS",
  lastAttemptAt: backup.startedAt,
  lastSuccessfulAt: backup.completedAt,
  lastFileName: backup.fileName,
  lastFileSizeBytes: backup.fileSizeBytes,
  lastFileSha256: null,
  backupDirectory: configuration.backupDirectory,
  retentionCount: configuration.retentionCount,
  message: "النسخ الاحتياطي جاهز",
};
const restoreWarning = "الاستعادة هترجّع بيانات المركز لحالة النسخة المختارة، وأي تغييرات بعدها مش هتظهر. هنعمل نسخة أمان الأول، والحفظ هيتوقف مؤقتًا لحد ما الاستعادة تخلص.";

async function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(<QueryClientProvider client={client}><MemoryRouter initialEntries={["/settings?section=backup"]}><SettingsPage /></MemoryRouter></QueryClientProvider>);
  await screen.findByRole("heading", { name: "سجل النسخ الاحتياطية" });
  await screen.findByLabelText("مجلد النسخ");
  await screen.findByRole("button", { name: "حذف" });
}

describe("إعدادات النسخ الاحتياطي المبسطة", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.hasAnyRole.mockReturnValue(true);
    mocks.getBackupStatus.mockResolvedValue(status);
    mocks.getBackupConfiguration.mockResolvedValue(configuration);
    mocks.getBackupHistory.mockResolvedValue([backup]);
    mocks.runManualBackup.mockResolvedValue(status);
    mocks.updateBackupConfiguration.mockResolvedValue(configuration);
    mocks.deleteBackup.mockResolvedValue(undefined);
    mocks.restoreBackup.mockResolvedValue(status);
  });

  it("يحذف البطاقات الإرشادية وفتح المجلد مع إبقاء أدوات النسخ والإعدادات والسجل", async () => {
    await renderPage();
    expect(screen.queryByText("ضمانات النسخ")).not.toBeInTheDocument();
    expect(screen.queryByText("الاستعادة بحذر")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "فتح المجلد" })).not.toBeInTheDocument();
    expect(screen.queryByText(restoreWarning)).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "إنشاء نسخة احتياطية الآن" })).toBeEnabled();
    expect(screen.getByLabelText("وقت النسخ اليومي")).toHaveValue("22:00");
    expect(screen.getByLabelText("عدد النسخ المحفوظة")).toHaveValue(14);
    expect(screen.getByLabelText("تفعيل النسخ اليومي التلقائي")).toBeChecked();
    expect(screen.getByRole("button", { name: "استعادة" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "حذف" })).toBeEnabled();
    expect(mocks.openBackupFolder).not.toHaveBeenCalled();
  });

  it("يحافظ على حفظ الجدولة والاحتفاظ والنسخ اليدوي بعد التأكيد", async () => {
    const user = userEvent.setup();
    await renderPage();
    await user.clear(screen.getByLabelText("عدد النسخ المحفوظة"));
    await user.type(screen.getByLabelText("عدد النسخ المحفوظة"), "30");
    await user.click(screen.getByLabelText("تفعيل النسخ اليومي التلقائي"));
    await user.click(screen.getByRole("button", { name: "حفظ الإعدادات" }));
    await waitFor(() => expect(mocks.updateBackupConfiguration).toHaveBeenCalledWith({ ...configuration, retentionCount: 30, automaticEnabled: false }));
    await user.click(screen.getByRole("button", { name: "إنشاء نسخة احتياطية الآن" }));
    expect(mocks.runManualBackup).not.toHaveBeenCalled();
    await user.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "بدء النسخ" }));
    await waitFor(() => expect(mocks.runManualBackup).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument());
  });

  it("يحافظ على تأكيد حذف النسخة المحددة", async () => {
    const user = userEvent.setup();
    await renderPage();
    await user.click(screen.getByRole("button", { name: "حذف" }));
    const dialog = screen.getByRole("alertdialog");
    expect(dialog).toHaveTextContent(backup.fileName);
    expect(mocks.deleteBackup).not.toHaveBeenCalled();
    await user.click(within(dialog).getByRole("button", { name: "حذف النسخة" }));
    await waitFor(() => expect(mocks.deleteBackup).toHaveBeenCalledWith(backup.id));
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument());
  });

  it("يعرض تحذير الاستعادة عند الإجراء فقط ويشترط كلمة المرور وتأكيد الملف دون تغيير الطلب", async () => {
    const user = userEvent.setup();
    await renderPage();
    await user.click(screen.getByRole("button", { name: "استعادة" }));
    expect(screen.getByRole("dialog")).toHaveTextContent(restoreWarning);
    await user.click(screen.getByRole("button", { name: "رجوع" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(mocks.restoreBackup).not.toHaveBeenCalled();

    await user.click(screen.getByRole("button", { name: "استعادة" }));
    const submit = screen.getByRole("button", { name: "استعادة الآن" });
    expect(submit).toBeDisabled();
    await user.type(screen.getByLabelText("كلمة مرور المالك"), "test-owner-password");
    expect(submit).toBeDisabled();
    await user.type(screen.getByLabelText(/اكتب للتأكيد/), "استعادة ملف آخر");
    expect(submit).toBeDisabled();
    await user.clear(screen.getByLabelText(/اكتب للتأكيد/));
    await user.type(screen.getByLabelText(/اكتب للتأكيد/), `استعادة ${backup.fileName}`);
    expect(submit).toBeEnabled();
    await user.click(submit);
    await waitFor(() => expect(mocks.restoreBackup).toHaveBeenCalledWith(backup.id, "test-owner-password", `استعادة ${backup.fileName}`));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(mocks.restoreBackup).toHaveBeenCalledTimes(1);
  });

  it("يرفض دخول غير المالك لصفحة النسخ حتى لو فتح رابطها مباشرة", async () => {
    mocks.hasAnyRole.mockReturnValue(false);
    render(<MemoryRouter initialEntries={["/settings?section=backup"]}><Routes>
      <Route path="/settings" element={<SettingsPage />} />
      <Route path="/forbidden" element={<p>ليس لديك صلاحية لهذه الصفحة</p>} />
    </Routes></MemoryRouter>);
    expect(await screen.findByText("ليس لديك صلاحية لهذه الصفحة")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "استعادة" })).not.toBeInTheDocument();
    expect(mocks.getBackupStatus).not.toHaveBeenCalled();
    expect(mocks.getBackupConfiguration).not.toHaveBeenCalled();
    expect(mocks.getBackupHistory).not.toHaveBeenCalled();
    expect(mocks.runManualBackup).not.toHaveBeenCalled();
    expect(mocks.restoreBackup).not.toHaveBeenCalled();
  });
});
