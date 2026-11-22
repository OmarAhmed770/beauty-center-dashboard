import { describe, expect, it } from "vitest";
import { availableEmployeeTransactionTypes, employeeSchema, employeeTransactionSchema } from "@/features/employees/model";

describe("نماذج الموظفين", () => {
  it("يتحقق من الحقول الأساسية للموظف", () => {
    expect(employeeSchema.safeParse({ name: "", phone: "", position: "", defaultDailyWage: "100", notes: "" }).success).toBe(false);
    expect(employeeSchema.safeParse({ name: "مريم", phone: "", position: "كوافيرة", defaultDailyWage: "350.50", notes: "" }).success).toBe(true);
  });

  it("يرفض مبلغ الحركة غير الموجب", () => {
    const result = employeeTransactionSchema.safeParse({ type: "DAILY_WAGE", amount: "0", businessDate: "2026-07-26", paymentMethod: "", notes: "" });
    expect(result.success).toBe(false);
  });

  it("يقصر موظف الاستقبال على الأجر اليومي", () => {
    expect(availableEmployeeTransactionTypes(["RECEPTIONIST"])).toEqual(["DAILY_WAGE"]);
    expect(availableEmployeeTransactionTypes(["RECEPTIONIST"], true)).toContain("PAYMENT");
  });
});
