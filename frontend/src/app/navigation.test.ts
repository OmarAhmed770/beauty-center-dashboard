import { describe, expect, it } from "vitest";
import { visibleAdvancedNavigation, visiblePrimaryNavigation } from "@/app/navigation";

describe("التنقل المبسط", () => {
  it("يعرض للموظف شاشة الأوردر وأوردرات النهارده فقط", () => {
    expect(visiblePrimaryNavigation(["ADD_ORDERS"]).map((item) => item.path)).toEqual(["/orders/new", "/orders/today"]);
    expect(visibleAdvancedNavigation(["ADD_ORDERS"])).toEqual([]);
  });

  it("يعرض للمالك ستة عناصر أساسية وثلاثة مداخل متقدمة فقط", () => {
    expect(visiblePrimaryNavigation(undefined, true).map((item) => item.path)).toEqual([
      "/dashboard",
      "/orders/new",
      "/expenses",
      "/catalog",
      "/employees",
      "/daily-closing",
    ]);
    expect(visibleAdvancedNavigation(undefined, true).map((item) => item.path)).toEqual([
      "/reports",
      "/sales",
      "/settings",
    ]);
  });

  it("يعرض الإعدادات لموظف النسخ الاحتياطي فقط دون فتح باقي الإدارات", () => {
    expect(visiblePrimaryNavigation(["BACKUP_MANAGEMENT"])).toEqual([]);
    expect(visibleAdvancedNavigation(["BACKUP_MANAGEMENT"]).map((item) => item.path)).toEqual(["/settings"]);
  });
});
