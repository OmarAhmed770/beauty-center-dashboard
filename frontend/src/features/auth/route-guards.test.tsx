import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PermissionRoute, ProtectedRoute } from "@/features/auth/route-guards";
import { navigationItems } from "@/app/navigation";

const { authState } = vi.hoisted(() => ({
  authState: {
    token: null as string | null,
    user: null as { displayName: string } | null,
    isLoading: false,
    currentUserError: null as unknown,
    retryCurrentUser: vi.fn(),
    hasAnyRole: vi.fn(() => false),
    hasPermission: vi.fn(() => false),
    hasAnyPermission: vi.fn(() => false),
  },
}));

vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => authState }));

describe("route guards", () => {
  beforeEach(() => {
    authState.token = null;
    authState.user = null;
    authState.isLoading = false;
    authState.currentUserError = null;
    authState.hasAnyRole.mockReturnValue(false);
    authState.hasPermission.mockReturnValue(false);
    authState.hasAnyPermission.mockReturnValue(false);
  });

  it("redirects an unauthenticated visitor to login", () => {
    render(
      <MemoryRouter initialEntries={["/employees"]}>
        <Routes>
          <Route path="/login" element={<p>شاشة تسجيل الدخول</p>} />
          <Route element={<ProtectedRoute />}><Route path="/employees" element={<p>بيانات الموظفين</p>} /></Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("شاشة تسجيل الدخول")).toBeInTheDocument();
    expect(screen.queryByText("بيانات الموظفين")).not.toBeInTheDocument();
  });

  it("redirects a user without an allowed role to the forbidden route", () => {
    render(
      <MemoryRouter initialEntries={["/reports"]}>
        <Routes>
          <Route path="/forbidden" element={<p>غير مصرح</p>} />
          <Route element={<PermissionRoute permission="REPORTS" />}><Route path="/reports" element={<p>التقارير الحساسة</p>} /></Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("غير مصرح")).toBeInTheDocument();
  });

  it("keeps advanced invoice management unavailable to a receptionist", () => {
    render(
      <MemoryRouter initialEntries={["/sales"]}>
        <Routes>
          <Route path="/forbidden" element={<p>غير مصرح</p>} />
          <Route element={<PermissionRoute permission="INVOICES_AND_RETURNS" />}><Route path="/sales" element={<p>إدارة الفواتير المتقدمة</p>} /></Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("غير مصرح")).toBeInTheDocument();
    expect(screen.queryByText("إدارة الفواتير المتقدمة")).not.toBeInTheDocument();
  });

  it("keeps catalog management unavailable to a receptionist by URL", () => {
    render(
      <MemoryRouter initialEntries={["/catalog"]}>
        <Routes>
          <Route path="/forbidden" element={<p>غير مصرح</p>} />
          <Route element={<PermissionRoute permission="SERVICES_AND_PRICES" />}><Route path="/catalog" element={<p>الخدمات والأسعار</p>} /></Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("غير مصرح")).toBeInTheDocument();
    expect(screen.queryByText("الخدمات والأسعار")).not.toBeInTheDocument();
  });

  it("allows the backup area when its explicit permission is present", () => {
    authState.hasAnyPermission.mockReturnValue(true);
    render(
      <MemoryRouter initialEntries={["/settings?section=backup"]}>
        <Routes>
          <Route path="/forbidden" element={<p>غير مصرح</p>} />
          <Route element={<PermissionRoute anyOf={["SETTINGS", "BACKUP_MANAGEMENT"]} />}><Route path="/settings" element={<p>النسخ الاحتياطي</p>} /></Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("النسخ الاحتياطي")).toBeInTheDocument();
  });

  it("allows an authenticated receptionist to open the simple-order route", () => {
    authState.token = "receptionist-token";
    authState.user = { displayName: "موظف الاستقبال" };
    render(
      <MemoryRouter initialEntries={["/orders/new"]}>
        <Routes>
          <Route element={<ProtectedRoute />}><Route path="/orders/new" element={<p>إضافة أوردر</p>} /></Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(navigationItems.find((item) => item.path === "/orders/new")?.permission).toBe("ADD_ORDERS");
    expect(screen.getByText("إضافة أوردر")).toBeInTheDocument();
  });
});
