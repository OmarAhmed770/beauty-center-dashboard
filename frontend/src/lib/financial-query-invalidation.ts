import type { QueryClient } from "@tanstack/react-query";

/**
 * Keeps the dashboard, reports, and live closing preview in sync after a financial write.
 * The backend remains the sole calculator; this only invalidates cached read models.
 */
export function invalidateFinancialViews(queryClient: QueryClient): Promise<unknown[]> {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ["reports"] }),
    queryClient.invalidateQueries({ queryKey: ["daily-closings"] }),
  ]);
}
