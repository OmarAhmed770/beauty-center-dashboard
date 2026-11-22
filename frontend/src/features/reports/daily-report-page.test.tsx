import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DailyReportPage } from "./daily-report-page";

const mocks = vi.hoisted(() => ({ demo: true, latest: vi.fn(), report: vi.fn() }));
vi.mock("@/lib/demo-mode", () => ({ get demoReadOnly() { return mocks.demo; } }));
vi.mock("@/components/shared/date-range-filter", () => ({ cairoDateIso: () => "2026-11-20" }));
vi.mock("@/features/business/currency-context", () => ({ useBusinessProfile: () => ({ profile: null }) }));
vi.mock("@/features/closing/api", () => ({ closingKeys: { latest: ["latest-closing"] }, getLatestDailyClosing: mocks.latest }));
vi.mock("@/features/reports/api", () => ({ reportKeys: { detail: (name: string, params: object) => [name, params] }, getDailyClosingReport: mocks.report }));

const snapshot = {
  id: 5, businessDate: "2026-11-19", closingVersion: 2, status: "CLOSED",
  grossSales: 4960, discounts: 0, refunds: 0, netSales: 4960, expenseTotal: 200,
  employeeCost: 820, costOfSoldItemsAndHair: 1220, netProfit: 2720,
  expectedCash: 3710, actualCash: 3710, cashDifference: 0, completedInvoiceCount: 7,
  paymentMethodBreakdown: [{ paymentMethodId: 1, code: "CASH", displayNameArabic: "نقدي", receivedAmount: 3530, refundedAmount: 0, netAmount: 3530 }],
  expenseCategoryBreakdown: [{ categoryId: 1, nameArabic: "مستلزمات تجريبية", expenseCount: 2, amount: 200 }],
  closedBy: "portfolio_owner", closedAt: "2026-11-19T20:00:00", reopenCount: 1, notes: "إقفال تجريبي محفوظ",
};

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><MemoryRouter><DailyReportPage /></MemoryRouter></QueryClientProvider>);
}

describe("demo daily closing report", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.demo = true;
    mocks.latest.mockResolvedValue(snapshot);
    mocks.report.mockImplementation((date: string) => Promise.resolve({ ...snapshot, businessDate: date }));
  });

  it("defaults demo to the latest saved date/version and renders its full stored report", async () => {
    renderPage();
    expect(await screen.findByText("ملخص الإغلاق اليومي")).toBeInTheDocument();
    expect(screen.getByLabelText("تاريخ العمل")).toHaveValue("2026-11-19");
    expect(mocks.report).toHaveBeenCalledWith("2026-11-19");
    expect(mocks.report).not.toHaveBeenCalledWith("2026-11-20");
    expect(screen.getByText(/النسخة 2/)).toBeInTheDocument();
    expect(screen.getByText("صافي الربح").parentElement).toHaveTextContent("2,720 ج.م");
    expect(screen.getByText("نقدي")).toBeInTheDocument();
    expect(screen.getByText("مستلزمات تجريبية")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "طباعة A4" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "إيصال حراري" })).toBeInTheDocument();
  });

  it("honors manual date selection and does not mislabel a missing day with another day's report", async () => {
    renderPage();
    await screen.findByText("ملخص الإغلاق اليومي");
    mocks.report.mockRejectedValueOnce(new Error("Daily closing not found."));
    fireEvent.change(screen.getByLabelText("تاريخ العمل"), { target: { value: "2026-11-20" } });
    expect(await screen.findByText("لا توجد لقطة إغلاق لهذا التاريخ")).toBeInTheDocument();
    expect(screen.getByLabelText("تاريخ العمل")).toHaveValue("2026-11-20");
    expect(screen.queryByText("ملخص الإغلاق اليومي")).not.toBeInTheDocument();
  });

  it("keeps the production default as today's business date without searching demo snapshots", async () => {
    mocks.demo = false;
    renderPage();
    await screen.findByText("ملخص الإغلاق اليومي");
    expect(screen.getByLabelText("تاريخ العمل")).toHaveValue("2026-11-20");
    expect(mocks.report).toHaveBeenCalledWith("2026-11-20");
    expect(mocks.latest).not.toHaveBeenCalled();
  });

  it("shows an honest empty state without creating a snapshot when no demo closing exists", async () => {
    mocks.latest.mockResolvedValue(null);
    renderPage();
    expect(await screen.findByText("لا توجد لقطة إغلاق محفوظة")).toBeInTheDocument();
    expect(mocks.report).not.toHaveBeenCalled();
  });

  it("shows a retryable lookup error and waits rather than requesting today's missing snapshot", async () => {
    mocks.latest.mockRejectedValue(new Error("تعذر الاتصال بالخادم"));
    renderPage();
    expect(await screen.findByText("تعذر الاتصال بالخادم")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole("button", { name: "إعادة المحاولة" })).toBeInTheDocument());
    expect(mocks.report).not.toHaveBeenCalled();
  });
});
