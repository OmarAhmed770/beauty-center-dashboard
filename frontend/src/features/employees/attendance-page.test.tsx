import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { EmployeeAttendancePage } from "@/features/employees/attendance-page";

const mocks = vi.hoisted(() => ({
  getEmployeeAttendance: vi.fn(),
  saveEmployeeAttendance: vi.fn(),
  invalidateFinancialViews: vi.fn(),
}));

vi.mock("@/features/employees/api", () => ({
  employeeKeys: {
    all: ["employees"],
    attendance: (businessDate: string) => ["employees", "attendance", businessDate],
  },
  getEmployeeAttendance: mocks.getEmployeeAttendance,
  saveEmployeeAttendance: mocks.saveEmployeeAttendance,
}));
vi.mock("@/lib/financial-query-invalidation", () => ({
  invalidateFinancialViews: mocks.invalidateFinancialViews,
}));

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><EmployeeAttendancePage /></QueryClientProvider>);
}

describe("حضور الموظفين", () => {
  it("يعرض الأجر والحالة ويسجل الحضور مرة واحدة عبر واجهة المالك", async () => {
    mocks.getEmployeeAttendance.mockResolvedValue([{
      id: null,
      employeeId: 7,
      employeeName: "منى",
      employeeActive: true,
      businessDate: "2026-11-14",
      status: null,
      dailyWageAmount: 300,
      dailyWageTransactionId: null,
      dailyWageTransactionStatus: null,
      version: 0,
      createdAt: null,
      updatedAt: null,
      createdBy: null,
      updatedBy: null,
    }]);
    mocks.saveEmployeeAttendance.mockResolvedValue({});
    const user = userEvent.setup();

    renderPage();
    expect(await screen.findByText("منى")).toBeInTheDocument();
    expect(screen.getByText(/300 ج.م/)).toBeInTheDocument();
    expect(screen.getByText("منى").parentElement).toHaveTextContent("لم يُسجل بعد");

    await user.click(screen.getByRole("button", { name: "حاضر" }));
    await waitFor(() => expect(mocks.saveEmployeeAttendance).toHaveBeenCalledWith(7, expect.any(String), "PRESENT"));
    expect(mocks.invalidateFinancialViews).toHaveBeenCalled();
  });
});
