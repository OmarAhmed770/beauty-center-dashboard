import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { clearAccessToken, readAccessToken, writeAccessToken } from "@/lib/auth-token";
import { requestCurrentUser, requestLogin } from "@/lib/api/auth";
import { parseApiError } from "@/lib/api/errors";
import type { LoginRequest, SystemPermission, UserResponse, UserRole } from "@/types/auth";

export type ServerStatus = "checking" | "online" | "offline";

interface AuthContextValue {
  token: string | null;
  user: UserResponse | null;
  isLoading: boolean;
  currentUserError: unknown;
  serverStatus: ServerStatus;
  login: (credentials: LoginRequest) => Promise<UserResponse>;
  logout: () => void;
  retryCurrentUser: () => void;
  hasAnyRole: (roles: readonly UserRole[]) => boolean;
  hasPermission: (permission: SystemPermission) => boolean;
  hasAnyPermission: (permissions: readonly SystemPermission[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);
export const currentUserQueryKey = ["auth", "current-user"] as const;

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [token, setToken] = useState<string | null>(readAccessToken);
  const [loginUser, setLoginUser] = useState<UserResponse | null>(null);

  const currentUserQuery = useQuery({
    queryKey: currentUserQueryKey,
    queryFn: requestCurrentUser,
    enabled: Boolean(token),
    retry: false,
    staleTime: 60_000,
    refetchInterval: 30_000,
  });

  const logout = useCallback(() => {
    clearAccessToken();
    setToken(null);
    setLoginUser(null);
    queryClient.removeQueries({ queryKey: currentUserQueryKey });
  }, [queryClient]);

  useEffect(() => {
    const handleUnauthorized = () => logout();
    window.addEventListener("beauty-center:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("beauty-center:unauthorized", handleUnauthorized);
  }, [logout]);

  const login = useCallback(
    async (credentials: LoginRequest) => {
      try {
        const response = await requestLogin(credentials);
        writeAccessToken(response.accessToken);
        setToken(response.accessToken);
        setLoginUser(response.user);
        queryClient.setQueryData(currentUserQueryKey, response.user);
        return response.user;
      } catch (error) {
        throw parseApiError(error);
      }
    },
    [queryClient],
  );

  const user = currentUserQuery.data ?? loginUser;
  const serverStatus: ServerStatus = !window.navigator.onLine
    ? "offline"
    : currentUserQuery.fetchStatus === "fetching"
      ? "checking"
      : currentUserQuery.isError
        ? "offline"
        : "online";

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isLoading: Boolean(token) && currentUserQuery.isPending && !user,
      currentUserError: currentUserQuery.error,
      serverStatus,
      login,
      logout,
      retryCurrentUser: () => void currentUserQuery.refetch(),
      hasAnyRole: (roles) => Boolean(user?.roles.some((role) => roles.includes(role))),
      hasPermission: (permission) => Boolean(user?.roles.includes("OWNER") || user?.permissions.includes(permission)),
      hasAnyPermission: (permissions) => Boolean(
        user?.roles.includes("OWNER") || user?.permissions.some((permission) => permissions.includes(permission)),
      ),
    }),
    [currentUserQuery.error, currentUserQuery.isPending, currentUserQuery.refetch, login, logout, serverStatus, token, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider.");
  return context;
}
