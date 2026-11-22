import { describe, expect, it } from "vitest";
import { isDemoWrite } from "./demo-mode";

describe("portfolio read-only requests", () => {
  it("keeps pure hair calculation available but refuses audited overrides", () => {
    const url = "/api/v1/hair-configurations/preview";
    expect(isDemoWrite("post", url, { configurationId: 1, quantity: 50, discountId: undefined })).toBe(false);
    expect(isDemoWrite("post", url, { configurationId: 1, quantity: 50, overrideAmount: 1 })).toBe(true);
    expect(isDemoWrite("post", url, { overrideReason: "change" })).toBe(true);
    expect(isDemoWrite("post", url, "{}")).toBe(true);
  });
  it("allows login and reading but blocks financial and account changes", () => {
    expect(isDemoWrite("POST", "/api/v1/auth/login")).toBe(false);
    expect(isDemoWrite("GET", "/api/v1/invoices")).toBe(false);
    for (const url of ["/api/v1/invoices/quick-orders", "/api/v1/backups/1/restore", "/api/v1/auth/register"]) {
      expect(isDemoWrite("POST", url)).toBe(true);
    }
    expect(isDemoWrite("PUT", "/api/v1/users/1")).toBe(true);
    expect(isDemoWrite("DELETE", "/api/v1/backups/1")).toBe(true);
  });
});
