import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PermissionGuard } from "@/components/shared/permission-guard";

const { hasAnyRole } = vi.hoisted(() => ({ hasAnyRole: vi.fn() }));
vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => ({ hasAnyRole }) }));

describe("PermissionGuard", () => {
  beforeEach(() => {
    hasAnyRole.mockReset();
  });

  it("renders protected content only for an allowed role", () => {
    hasAnyRole.mockReturnValue(false);
    const { rerender } = render(<PermissionGuard roles={["OWNER"]} fallback={<span>بدون صلاحية</span>}><button>إدارة المستخدمين</button></PermissionGuard>);
    expect(screen.getByText("بدون صلاحية")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "إدارة المستخدمين" })).not.toBeInTheDocument();

    hasAnyRole.mockReturnValue(true);
    rerender(<PermissionGuard roles={["OWNER"]} fallback={<span>بدون صلاحية</span>}><button>إدارة المستخدمين</button></PermissionGuard>);
    expect(screen.getByRole("button", { name: "إدارة المستخدمين" })).toBeInTheDocument();
  });
});
