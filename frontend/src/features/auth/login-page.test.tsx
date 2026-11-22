// TEST ONLY - credential literals in this file are synthetic test fixtures.
import { fireEvent, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LoginPage } from "@/features/auth/login-page";

const { loginMock, identityMock } = vi.hoisted(() => ({ loginMock: vi.fn(), identityMock: vi.fn() }));

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ token: null, user: null, login: loginMock }),
}));

vi.mock("@/lib/api/auth", () => ({ requestPublicBusinessIdentity: identityMock }));

function renderPage(children: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{children}</QueryClientProvider>);
}

describe("LoginPage", () => {
  beforeEach(() => {
    loginMock.mockReset();
    identityMock.mockResolvedValue({ businessName: "مركز الاختبار", logoDataUrl: null });
  });

  it("shows Arabic validation messages when credentials are empty", async () => {
    renderPage(<MemoryRouter><LoginPage /></MemoryRouter>);

    fireEvent.submit(screen.getByRole("form", { name: "نموذج تسجيل الدخول" }));

    expect(await screen.findByText("اسم المستخدم مطلوب")).toBeInTheDocument();
    expect(screen.getByText("كلمة المرور مطلوبة")).toBeInTheDocument();
    expect(loginMock).not.toHaveBeenCalled();
  });

  it("displays the parsed authentication API error", async () => {
    loginMock.mockRejectedValue({
      isAxiosError: true,
      response: {
        data: {
          status: 401,
          code: "AUTHENTICATION_FAILED",
          message: "اسم المستخدم أو كلمة المرور غير صحيحة",
        },
      },
    });
    const user = userEvent.setup();
    renderPage(<MemoryRouter><LoginPage /></MemoryRouter>);

    await user.type(screen.getByLabelText("اسم المستخدم"), "reception");
    await user.type(screen.getByLabelText("كلمة المرور"), "invalid-password");
    fireEvent.submit(screen.getByRole("form", { name: "نموذج تسجيل الدخول" }));

    expect(await screen.findByText("اسم المستخدم أو كلمة المرور غير صحيحة")).toHaveAttribute("role", "alert");
  });

  it("does not expose an unknown English server error", async () => {
    loginMock.mockRejectedValue({
      isAxiosError: true,
      response: { data: { status: 500, code: "UNKNOWN", message: "An unexpected error occurred." } },
    });
    const user = userEvent.setup();
    renderPage(<MemoryRouter><LoginPage /></MemoryRouter>);

    await user.type(screen.getByLabelText("اسم المستخدم"), "reception");
    await user.type(screen.getByLabelText("كلمة المرور"), "valid-password");
    fireEvent.submit(screen.getByRole("form", { name: "نموذج تسجيل الدخول" }));

    expect(await screen.findByText("حصلت مشكلة، جرّب تاني")).toHaveAttribute("role", "alert");
    expect(screen.queryByText("An unexpected error occurred.")).not.toBeInTheDocument();
  });

  it("opens the simple order screen after a receptionist signs in", async () => {
    loginMock.mockResolvedValue({
      id: 7,
      username: "reception",
      displayName: "موظف الاستقبال",
      roles: ["RECEPTIONIST"],
      permissions: ["ADD_ORDERS"],
      active: true,
      createdAt: "2026-08-01T09:00:00",
      updatedAt: "2026-08-01T09:00:00",
      createdBy: null,
      updatedBy: null,
    });
    const user = userEvent.setup();
    renderPage(
      <MemoryRouter initialEntries={["/login"]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/orders/new" element={<p>إضافة أوردر</p>} />
        </Routes>
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText("اسم المستخدم"), "reception");
    await user.type(screen.getByLabelText("كلمة المرور"), "valid-password");
    fireEvent.submit(screen.getByRole("form", { name: "نموذج تسجيل الدخول" }));

    expect(await screen.findByText("إضافة أوردر")).toBeInTheDocument();
  });

});
