import { Navigate, Outlet, useLocation } from "react-router-dom";
import { ErrorState } from "@/components/shared/error-state";
import { LoadingState } from "@/components/shared/loading-state";
import { useAuth } from "@/features/auth/auth-context";
import type { SystemPermission, UserRole } from "@/types/auth";

export function ProtectedRoute() {
  const { token, user, isLoading, currentUserError, retryCurrentUser } = useAuth();
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (isLoading) {
    return <LoadingState fullPage label="جارٍ تحميل بيانات المستخدم..." />;
  }

  if (!user && currentUserError) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-stone-50 p-6">
        <ErrorState error={currentUserError} onRetry={retryCurrentUser} />
      </div>
    );
  }

  return user ? <Outlet /> : <Navigate to="/login" replace />;
}

export function RoleRoute({ allowedRoles }: { allowedRoles: readonly UserRole[] }) {
  const { hasAnyRole } = useAuth();
  return hasAnyRole(allowedRoles) ? <Outlet /> : <Navigate to="/forbidden" replace />;
}

export function PermissionRoute({ permission, anyOf }: { permission?: SystemPermission; anyOf?: readonly SystemPermission[] }) {
  const { hasPermission, hasAnyPermission } = useAuth();
  const allowed = permission ? hasPermission(permission) : Boolean(anyOf && hasAnyPermission(anyOf));
  return allowed ? <Outlet /> : <Navigate to="/forbidden" replace />;
}
