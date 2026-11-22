// TEST ONLY - credential literals in this file are synthetic test fixtures.
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { StaffRegistrationPage } from "@/features/auth/staff-registration-page";

const { identityMock, registrationMock } = vi.hoisted(() => ({ identityMock: vi.fn(), registrationMock: vi.fn() }));

vi.mock("@/lib/api/auth", () => ({
  requestPublicBusinessIdentity: identityMock,
  requestStaffRegistration: registrationMock,
}));

function renderPage(children: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><MemoryRouter>{children}</MemoryRouter></QueryClientProvider>);
}

describe("StaffRegistrationPage", () => {
  beforeEach(() => {
    identityMock.mockResolvedValue({ businessName: "مركز الاختبار", logoDataUrl: null });
    registrationMock.mockReset();
  });

  it("shows Arabic validation errors without submitting incomplete requests", async () => {
    renderPage(<StaffRegistrationPage />);

    fireEvent.submit(screen.getByRole("form", { name: "نموذج إنشاء حساب موظف" }));

    expect(await screen.findByText("الاسم الكامل مطلوب")).toBeInTheDocument();
    expect(screen.getByText("استخدم 3 أحرف صغيرة أو أرقام أو . أو _ أو -")).toBeInTheDocument();
    expect(registrationMock).not.toHaveBeenCalled();
  });

  it("submits a pending staff request without a role and confirms the wait state", async () => {
    registrationMock.mockResolvedValue({ username: "staff.user", registrationStatus: "PENDING" });
    const user = userEvent.setup();
    renderPage(<StaffRegistrationPage />);

    await user.type(screen.getByLabelText(/^الاسم الكامل/), "موظف جديد");
    await user.type(screen.getByLabelText(/^اسم المستخدم/), "staff.user");
    await user.type(screen.getByLabelText(/^كلمة المرور/), "securepassword12");
    await user.type(screen.getByLabelText(/^تأكيد كلمة المرور/), "securepassword12");
    fireEvent.submit(screen.getByRole("form", { name: "نموذج إنشاء حساب موظف" }));

    expect(await screen.findByText(/تم إرسال طلب الحساب/)).toBeInTheDocument();
    expect(registrationMock).toHaveBeenCalledTimes(1);
    expect(registrationMock.mock.calls[0][0]).toEqual({
      displayName: "موظف جديد",
      username: "staff.user",
      password: "securepassword12",
      passwordConfirmation: "securepassword12",
      phone: undefined,
    });
  });
});
