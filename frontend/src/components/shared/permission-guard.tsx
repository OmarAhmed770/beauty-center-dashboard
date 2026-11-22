import type { ReactNode } from "react";
import { useAuth } from "@/features/auth/auth-context";
import type { UserRole } from "@/types/auth";

export function PermissionGuard({
  roles,
  children,
  fallback = null,
}: {
  roles: readonly UserRole[];
  children: ReactNode;
  fallback?: ReactNode;
}) {
  const { hasAnyRole } = useAuth();
  return hasAnyRole(roles) ? <>{children}</> : <>{fallback}</>;
}
