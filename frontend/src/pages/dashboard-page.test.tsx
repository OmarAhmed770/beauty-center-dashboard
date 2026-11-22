import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardPage } from "@/pages/dashboard-page";

const { getDailyClosingPreviewMock, getDashboardMock, getInvoiceMock, getPendingRegistrationsMock, hasAnyRoleMock, searchHairConfigurationsMock, searchInvoicesMock } = vi.hoisted(() => ({
  getDailyClosingPreviewMock: vi.fn(),
  getDashboardMock: vi.fn(),
  getInvoiceMock: vi.fn(),
  getPendingRegistrationsMock: vi.fn(),
  hasAnyRoleMock: vi.fn(),
  searchHairConfigurationsMock: vi.fn(),
  searchInvoicesMock: vi.fn(),
}));

vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => ({ hasAnyRole: hasAnyRoleMock }) }));
vi.mock("@/features/closing/api", () => ({ getDailyClosingPreview: getDailyClosingPreviewMock, closingKeys: { preview: (date: string) => ["daily-closing", "preview", date] } }));
vi.mock("@/features/hair/api", () => ({ searchHairConfigurations: searchHairConfigurationsMock, hairKeys: { list: (params: object) => ["hair", params] } }));
vi.mock("@/features/reports/api", () => ({
  getDashboard: getDashboardMock,
  reportKeys: { dashboard: (params: object) => ["reports", "dashboard", params] },
}));
vi.mock("@/features/sales/api", () => ({
  getInvoice: getInvoiceMock,
  searchInvoices: searchInvoicesMock,
  invoiceKeys: { list: (params: object) => ["invoices", "list", params], detail: (id: number) => ["invoices", "detail", id] },
}));
vi.mock("@/features/users/api", () => ({ getPendingRegistrations: getPendingRegistrationsMock, userKeys: { pending: ["users", "pending-registrations"] } }));

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><MemoryRouter><DashboardPage /></MemoryRouter></QueryClientProvider>);
}

describe("لوحة المالك المبسطة", () => {
  beforeEach(() => {
    hasAnyRoleMock.mockReturnValue(true);
    getPendingRegistrationsMock.mockResolvedValue([]);
    getDailyClosingPreviewMock.mockResolvedValue({ status: "OPEN" });
    searchHairConfigurationsMock.mockResolvedValue({ content: [] });
    getDashboardMock.mockImplementation((params: { filter: string }) => Promise.resolve(params.filter === "CURRENT_MONTH"
      ? { grossSales: 1800, expenses: 250, netProfit: 1150, paymentTotalsByMethod: [], topServicesAndProducts: [] }
      : params.filter === "CUSTOM"
        ? { grossSales: 1200, expenses: 175, netProfit: 725, paymentTotalsByMethod: [], topServicesAndProducts: [] }
        : { grossSales: 900, expenses: 125, netProfit: 520, expectedCashBalance: 500, completedInvoiceCount: 3, paymentTotalsByMethod: [{ type: "CARD", netAmount: 400 }], topServicesAndProducts: [{ itemNameArabic: "صبغة شعر" }] }));
    searchInvoicesMock.mockResolvedValue({ content: [{ id: 4, customerName: "سارة", finalTotal: 300, completedAt: new Date().toISOString(), createdAt: new Date().toISOString() }] });
    getInvoiceMock.mockResolvedValue({ id: 4, items: [{ itemNameArabic: "صبغة شعر", employeeName: "منى" }], payments: [{ paymentMethodNameArabic: "نقدي" }] });
  });

  it("يعرض لوحة المالك الموحدة بالقيم المعتمدة من الخادم وإجراءات العمل الأساسية", async () => {
    renderPage();
    expect(await screen.findByText("النهارده")).toBeInTheDocument();
    expect(screen.getByText("مبيعات النهارده")).toBeInTheDocument();
    expect(screen.getByText("المصروفات")).toBeInTheDocument();
    expect(screen.getAllByText("صافي الربح")[0].parentElement).toHaveTextContent("520 ج.م");
    expect(screen.getByText("عدد الأوردرات").parentElement).toHaveTextContent("3");
    expect(screen.getByText("الكاش المتوقع").parentElement).toHaveTextContent("500 ج.م");
    expect(screen.getByText("المدفوعات الإلكترونية").parentElement).toHaveTextContent("400 ج.م");
    expect(screen.getByText("الشهر ده").parentElement).toHaveTextContent("1,800 ج.م");
    expect(screen.getByText("مقارنة بالشهر السابق:")).toHaveTextContent("+425 ج.م");
    expect(screen.getByRole("link", { name: "إضافة أوردر" })).toHaveAttribute("href", "/orders/new");
    expect((await screen.findAllByText("صبغة شعر")).length).toBeGreaterThan(0);
    expect(screen.getByText("كل الأوردرات")).toHaveAttribute("href", "/sales");
  });

  it("يعرض طلبات التسجيل فقط ولا يعرض تنبيه النسخ الاحتياطي على اللوحة", async () => {
    getPendingRegistrationsMock.mockResolvedValue([{ id: 1 }, { id: 2 }]);
    renderPage();
    expect(screen.queryByText("النسخ الاحتياطي مش شغال")).not.toBeInTheDocument();
    expect(await screen.findByText("في 2 طلبات تسجيل منتظرة")).toBeInTheDocument();
  });
});
