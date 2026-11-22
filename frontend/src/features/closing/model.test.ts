import { describe, expect, it } from "vitest";
import { calculateCashDifference, validateActualCash } from "@/features/closing/model";

describe("daily closing model", () => {
  it("validates non-negative actual cash with currency precision", () => {
    expect(validateActualCash("0.00")).toBeNull();
    expect(validateActualCash("1500.25")).toBeNull();
    expect(validateActualCash("-1")).not.toBeNull();
    expect(validateActualCash("1.234")).not.toBeNull();
  });

  it("calculates the cash difference without confusing it with profit", () => {
    expect(calculateCashDifference("350", "340.00")).toBe(10);
    expect(calculateCashDifference("330", 340)).toBe(-10);
  });
});
