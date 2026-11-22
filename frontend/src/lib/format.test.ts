import { afterEach, describe, expect, it } from "vitest";
import { formatArabicDate, formatMoney, formatNumber, formatQuantity, getActiveCurrencySymbol, setActiveCurrency } from "@/lib/format";

describe("business currency formatting", () => {
  afterEach(() => setActiveCurrency("EGP"));

  it("formats the same stored amount with the configured currency without altering its value", () => {
    setActiveCurrency("SAR");

    expect(formatMoney("125.50")).toBe("125.50 ر.س");
    expect(getActiveCurrencySymbol()).toBe("ر.س");
  });

  it("switches displayed currency only", () => {
    setActiveCurrency("USD");
    const usd = formatMoney(20);
    setActiveCurrency("EGP");
    const egp = formatMoney(20);

    expect(usd).not.toBe(egp);
  });

  it("uses western digits, grouped amounts, and avoids unnecessary decimals", () => {
    expect(formatMoney(1250)).toBe("1,250 ج.م");
    expect(formatMoney(1250.5)).toBe("1,250.50 ج.م");
    expect(formatNumber(1250)).toBe("1,250");
  });

  it("removes operational quantity trailing zeros without changing money precision", () => {
    expect(formatQuantity("1.000")).toBe("1");
    expect(formatQuantity("2.000")).toBe("2");
    expect(formatQuantity("1.500")).toBe("1.5");
    expect(formatMoney("1.50")).toBe("1.50 ج.م");
  });

  it("uses concise Arabic dates with western digits", () => {
    expect(formatArabicDate("2026-11-13")).toBe("13 نوفمبر");
  });
});
