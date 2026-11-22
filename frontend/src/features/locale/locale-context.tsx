import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

export type Locale = "ar" | "en";

const messages = {
  ar: {
    appName: "نظام الإدارة",
    dashboard: "لوحة التحكم",
    addOrder: "إضافة أوردر",
    todayOrders: "أوردرات النهارده",
    advancedInvoices: "إدارة الفواتير المتقدمة",
    invoicesRefunds: "الفواتير والمرتجعات",
    employees: "الموظفون",
    expenses: "المصروفات",
    catalog: "الخدمات والمنتجات",
    servicesPrices: "الخدمات والأسعار",
    discounts: "الخصومات",
    scheduledDiscounts: "الخصومات المجدولة",
    hairInstallation: "تركيب الشعر",
    priceHistory: "سجل الأسعار",
    expenseSettings: "إعدادات المصروفات والدفع",
    serviceCategories: "تصنيفات الخدمات",
    advanced: "متقدم",
    reports: "التقارير",
    closing: "الإقفال اليومي",
    settings: "الإعدادات",
    users: "المستخدمون",
    logout: "تسجيل الخروج",
    businessDate: "تاريخ العمل",
    lastBackup: "آخر نسخة احتياطية",
    unavailable: "غير متاح حاليًا",
    serverOnline: "الخادم متصل",
    serverOffline: "الخادم غير متصل",
    serverChecking: "فحص الاتصال",
    dailyOperations: "العمليات اليومية",
    management: "الإدارة والمتابعة",
    administration: "إعدادات النظام",
  },
  en: {
    appName: "Management System",
    dashboard: "Dashboard",
    addOrder: "Add order",
    todayOrders: "Today's orders",
    advancedInvoices: "Advanced invoices",
    invoicesRefunds: "Invoices & refunds",
    employees: "Employees",
    expenses: "Expenses",
    catalog: "Catalog",
    servicesPrices: "Services & prices",
    discounts: "Discounts",
    scheduledDiscounts: "Scheduled discounts",
    hairInstallation: "Hair installation",
    priceHistory: "Price history",
    expenseSettings: "Expense & payment settings",
    serviceCategories: "Service categories",
    advanced: "Advanced",
    reports: "Reports",
    closing: "Daily Closing",
    settings: "Settings",
    users: "Users",
    logout: "Log out",
    businessDate: "Business date",
    lastBackup: "Last backup",
    unavailable: "Not available yet",
    serverOnline: "Server online",
    serverOffline: "Server offline",
    serverChecking: "Checking server",
    dailyOperations: "Daily operations",
    management: "Management",
    administration: "Administration",
  },
} as const;

export type TranslationKey = keyof (typeof messages)["ar"];

interface LocaleContextValue {
  locale: Locale;
  direction: "rtl" | "ltr";
  setLocale: (locale: Locale) => void;
  toggleLocale: () => void;
  t: (key: TranslationKey) => string;
}

const LocaleContext = createContext<LocaleContextValue | null>(null);
export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>("ar");

  useEffect(() => {
    document.documentElement.lang = locale;
    document.documentElement.dir = locale === "ar" ? "rtl" : "ltr";
  }, [locale]);

  const value = useMemo<LocaleContextValue>(
    () => ({
      locale,
      direction: locale === "ar" ? "rtl" : "ltr",
      setLocale: () => setLocaleState("ar"),
      toggleLocale: () => setLocaleState("ar"),
      t: (key) => messages[locale][key],
    }),
    [locale],
  );

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocale(): LocaleContextValue {
  const context = useContext(LocaleContext);
  if (!context) throw new Error("useLocale must be used inside LocaleProvider.");
  return context;
}
