import { useQuery } from "@tanstack/react-query";
import { createContext, Fragment, useContext, type ReactNode } from "react";
import { useAuth } from "@/features/auth/auth-context";
import { businessProfileKeys, getBusinessProfile } from "@/features/business/api";
import type { BusinessProfile } from "@/features/business/types";
import { getActiveCurrency, setActiveCurrency, type CurrencyCode } from "@/lib/format";

interface CurrencyContextValue {
  profile: BusinessProfile | undefined;
  currency: CurrencyCode;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
}

const CurrencyContext = createContext<CurrencyContextValue | null>(null);

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  const profileQuery = useQuery({
    queryKey: businessProfileKeys.current,
    queryFn: getBusinessProfile,
    enabled: Boolean(token),
    staleTime: 5 * 60_000,
  });
  const currency = profileQuery.data?.currency ?? "EGP";
  setActiveCurrency(currency);

  return (
    <CurrencyContext.Provider value={{
      profile: profileQuery.data,
      currency,
      isLoading: profileQuery.isPending,
      isError: profileQuery.isError,
      refetch: () => void profileQuery.refetch(),
    }}>
      <Fragment key={currency}>{children}</Fragment>
    </CurrencyContext.Provider>
  );
}

export function useBusinessProfile(): CurrencyContextValue {
  const context = useContext(CurrencyContext);
  if (!context) throw new Error("useBusinessProfile must be used inside CurrencyProvider.");
  return context;
}

export function useBusinessCurrency(): CurrencyCode {
  return useBusinessProfile().currency ?? getActiveCurrency();
}
