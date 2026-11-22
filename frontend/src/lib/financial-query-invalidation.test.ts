import { describe, expect, it, vi } from "vitest";
import { invalidateFinancialViews } from "@/lib/financial-query-invalidation";

describe("invalidateFinancialViews", () => {
  it("refreshes report data and every daily-closing read model after a financial write", async () => {
    const invalidateQueries = vi.fn().mockResolvedValue(undefined);

    await invalidateFinancialViews({ invalidateQueries } as never);

    expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ["reports"] });
    expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ["daily-closings"] });
  });
});
