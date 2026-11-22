export type MoneyValue = number | string;
export type CurrencyCode = "EGP" | "SAR" | "AED" | "USD" | "EUR";

const defaultCurrency: CurrencyCode = "EGP";
let activeCurrency: CurrencyCode = defaultCurrency;
const formatters = new Map<string, Intl.NumberFormat>();
const dateFormatter = new Intl.DateTimeFormat("ar-EG-u-nu-latn", {
  month: "short",
  day: "numeric",
  timeZone: "Africa/Cairo",
});
const timeFormatter = new Intl.DateTimeFormat("ar-EG-u-nu-latn", {
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "Africa/Cairo",
});
const currencySymbols: Record<CurrencyCode, string> = {
  EGP: "ج.م",
  SAR: "ر.س",
  AED: "د.إ",
  USD: "US$",
  EUR: "€",
};

export function setActiveCurrency(currency: CurrencyCode | null | undefined): void {
  activeCurrency = currency ?? defaultCurrency;
}

export function getActiveCurrency(): CurrencyCode {
  return activeCurrency;
}

export function getActiveCurrencySymbol(): string {
  return currencySymbols[activeCurrency];
}

function formatter(currency: CurrencyCode, fractionDigits: 0 | 2): Intl.NumberFormat {
  const key = `${currency}:${fractionDigits}`;
  const existing = formatters.get(key);
  if (existing) return existing;
  const created = new Intl.NumberFormat("en-US", {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
  formatters.set(key, created);
  return created;
}

export function formatMoney(value: MoneyValue | null | undefined): string {
  const numericValue = typeof value === "string" ? Number(value) : value;
  const numeric = Number.isFinite(numericValue) ? numericValue! : 0;
  return `${formatter(activeCurrency, Number.isInteger(numeric) ? 0 : 2).format(numeric)} ${currencySymbols[activeCurrency]}`;
}

/** @deprecated Use formatMoney. Kept as a compatibility alias while financial screens share one formatter. */
export const formatEgp = formatMoney;

export function formatNumber(value: MoneyValue | null | undefined): string {
  const numericValue = typeof value === "string" ? Number(value) : value;
  return new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(Number.isFinite(numericValue) ? numericValue! : 0);
}

/** Quantities are operational counts/grams, not money. Keeps historical fractional data readable. */
export function formatQuantity(value: MoneyValue | null | undefined): string {
  const numericValue = typeof value === "string" ? Number(value) : value;
  const numeric = Number.isFinite(numericValue) ? numericValue! : 0;
  return new Intl.NumberFormat("en-US", { maximumFractionDigits: 3 }).format(numeric);
}

export function formatArabicDate(value: string | null | undefined): string {
  if (!value) return "—";
  return dateFormatter.format(new Date(`${value.slice(0, 10)}T12:00:00+02:00`));
}

export function formatArabicDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  const now = new Date();
  const cairoDay = new Intl.DateTimeFormat("en-CA", { timeZone: "Africa/Cairo" });
  if (cairoDay.format(date) === cairoDay.format(now)) return timeFormatter.format(date);
  return `${dateFormatter.format(date)} · ${timeFormatter.format(date)}`;
}
