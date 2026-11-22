import { ChevronDown, CirclePlus, LogOut, Menu, ReceiptText, Sparkles, WalletCards, X } from "lucide-react";
import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { navigationItems, visibleAdvancedNavigation, visiblePrimaryNavigation } from "@/app/navigation";
import { useAuth } from "@/features/auth/auth-context";
import { useBusinessProfile } from "@/features/business/currency-context";
import { useLocale } from "@/features/locale/locale-context";
import { cn } from "@/lib/utils";
import { demoReadOnly } from "@/lib/demo-mode";
import type { UserRole } from "@/types/auth";

const roleLabels: Record<UserRole, { ar: string; en: string }> = {
  OWNER: { ar: "مالك", en: "Owner" },
  RECEPTIONIST: { ar: "موظف", en: "Staff" },
};

const hiddenRouteTitles: Array<{ path: string; title: string }> = [
  { path: "/sales", title: "الفواتير والمرتجعات" },
  { path: "/reports", title: "التقارير" },
  { path: "/discounts", title: "الخصومات المجدولة" },
  { path: "/catalog/hair", title: "تركيب الشعر" },
  { path: "/catalog/items", title: "سجل الأسعار" },
  { path: "/expenses/settings", title: "إعدادات المصروفات وطرق الدفع" },
  { path: "/catalog/categories", title: "تصنيفات الخدمات" },
  { path: "/users", title: "المستخدمون والموافقات" },
];

export function AppShell() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const { user, logout, serverStatus } = useAuth();
  const { profile } = useBusinessProfile();
  const { direction, t } = useLocale();
  const location = useLocation();
  const activeItem = [...navigationItems].sort((left, right) => right.path.length - left.path.length).find((item) => location.pathname.startsWith(item.path));
  const hiddenRouteTitle = [...hiddenRouteTitles].sort((left, right) => right.path.length - left.path.length).find((item) => location.pathname.startsWith(item.path))?.title;
  const roleLabel = user?.roles.map((role) => roleLabels[role].ar).join("، ");
  const owner = Boolean(user?.roles.includes("OWNER"));
  const primaryItems = visiblePrimaryNavigation(user?.permissions, owner);
  const advancedItems = visibleAdvancedNavigation(user?.permissions, owner);
  const staffOnly = !owner && primaryItems.length <= 2 && advancedItems.length === 0;
  const businessName = profile?.businessName || t("appName");

  useEffect(() => {
    if (!sidebarOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [sidebarOpen]);

  const sidebar = (
    <aside className="flex h-[100dvh] w-[min(20rem,calc(100vw-2.5rem))] flex-col overflow-hidden border-e border-soft-border bg-sidebar text-ink shadow-[-8px_0_28px_rgb(58_37_48_/_0.04)] lg:h-full lg:w-80">
      <div className="flex h-20 items-center gap-3 border-b border-soft-border px-6">
        {profile?.logoDataUrl ? <img className="h-11 w-11 shrink-0 rounded-2xl border border-soft-border bg-white object-contain" src={profile.logoDataUrl} alt={`شعار ${businessName}`} /> : <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-primary text-white shadow-lg shadow-primary/20"><Sparkles className="h-5 w-5" aria-hidden="true" /></span>}
        <p className="min-w-0 truncate font-black text-ink">{businessName}</p>
      </div>

      <nav className="flex-1 space-y-4 overflow-y-auto overscroll-contain px-4 py-5" aria-label="التنقل الرئيسي">
        <div className="space-y-1">{primaryItems.map((item) => <NavLink key={item.path} to={item.path} onClick={() => setSidebarOpen(false)} className={({ isActive }) => cn("flex min-h-12 items-center gap-3 rounded-2xl px-4 text-sm font-bold text-muted transition-colors hover:bg-white/75 hover:text-ink", isActive && "bg-primary-soft text-primary shadow-sm")}><item.icon className="h-5 w-5 shrink-0" aria-hidden="true" /><span>{t(item.labelKey)}</span></NavLink>)}</div>
        {!staffOnly && advancedItems.length > 0 && <details className="group rounded-2xl border border-soft-border bg-white/70" open={advancedOpen} onToggle={(event) => setAdvancedOpen((event.currentTarget as HTMLDetailsElement).open)}><summary className="flex min-h-12 cursor-pointer list-none items-center justify-between gap-3 px-4 text-sm font-black text-muted"><span>{t("advanced")}</span><ChevronDown className="h-4 w-4 transition-transform group-open:rotate-180" /></summary><div className="space-y-1 border-t border-soft-border p-2">{advancedItems.map((item) => <NavLink key={item.path} to={item.path} onClick={() => setSidebarOpen(false)} className={({ isActive }) => cn("flex min-h-11 items-center gap-3 rounded-xl px-3 text-sm font-bold text-muted hover:bg-white/75 hover:text-ink", isActive && "bg-primary-soft text-primary")}><item.icon className="h-4 w-4 shrink-0" aria-hidden="true" /><span>{t(item.labelKey)}</span></NavLink>)}</div></details>}
      </nav>
    </aside>
  );

  return (
    <div className="min-h-screen bg-content" dir={direction}>
      <div className={cn("fixed inset-y-0 z-50 hidden lg:block", direction === "rtl" ? "right-0" : "left-0")}>{sidebar}</div>

      {sidebarOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button className="absolute inset-0 bg-stone-950/50" aria-label="إغلاق القائمة" onClick={() => setSidebarOpen(false)} />
          <div className={cn("absolute inset-y-0", direction === "rtl" ? "right-0" : "left-0")}>{sidebar}</div>
          <button className={cn("absolute top-4 grid h-11 w-11 place-items-center rounded-full bg-white text-[#29252A] shadow-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A83868]", direction === "rtl" ? "right-[calc(min(20rem,100vw-2.5rem)-1.25rem)]" : "left-[calc(min(20rem,100vw-2.5rem)-1.25rem)]")} aria-label="إغلاق القائمة" onClick={() => setSidebarOpen(false)}><X className="h-5 w-5" /></button>
        </div>
      )}

      <div className={cn("min-h-screen bg-content", direction === "rtl" ? "lg:mr-80" : "lg:ml-80")}>
        <header className="sticky top-0 z-30 flex min-h-16 items-center justify-between gap-2 border-b border-soft-border bg-white/90 px-3 shadow-[0_1px_10px_rgb(58_37_48_/_0.03)] backdrop-blur sm:min-h-20 sm:gap-4 sm:px-6 lg:px-8">
          <h1 className="min-w-0 truncate text-sm font-black text-ink sm:text-base">{hiddenRouteTitle || (activeItem ? t(activeItem.labelKey) : businessName)}</h1>

          <div className="flex shrink-0 items-center gap-1 sm:gap-2">
            <details className="group relative">
              <summary className="flex min-h-11 cursor-pointer list-none items-center gap-2 rounded-2xl px-2 hover:bg-[#f6f5f5] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A83868]">
                <span className="grid h-9 w-9 place-items-center rounded-xl bg-[#FCEAF2] font-black text-[#A83868]">{user?.displayName.trim().charAt(0) || "م"}</span>
                <span className="hidden max-w-32 truncate text-sm font-bold text-[#4b454b] sm:block">{user?.displayName}</span>
                <ChevronDown className="hidden h-4 w-4 text-[#a69aa2] sm:block" />
              </summary>
              <div className={cn("absolute top-14 w-64 rounded-2xl border border-[#eee7ea] bg-white p-2 shadow-xl", direction === "rtl" ? "left-0" : "right-0")}>
                <div className="border-b border-[#f1ebee] px-3 py-3"><p className="font-black text-[#29252A]">{user?.displayName}</p><p className="mt-1 text-xs text-[#736873]">{user?.username} · {roleLabel}</p></div>
                <button type="button" className="mt-1 flex w-full items-center gap-2 rounded-xl px-3 py-3 text-sm font-bold text-red-700 hover:bg-red-50" onClick={logout}><LogOut className="h-4 w-4" />{t("logout")}</button>
              </div>
            </details>
          </div>
        </header>

        {demoReadOnly && <p className="mx-3 mt-3 rounded-xl bg-primary-soft px-4 py-2 text-sm font-bold text-primary sm:mx-6 lg:mx-8" role="status">نسخة تجريبية ببيانات وهمية — للعرض فقط، الحفظ والتعديل غير متاحين.</p>}
        {serverStatus === "offline" && <p className="mx-3 mt-3 rounded-xl bg-red-50 px-4 py-2 text-sm font-bold text-red-700 sm:mx-6 lg:mx-8" role="alert">مفيش اتصال بالسيرفر</p>}
        <main className="mx-auto w-full max-w-[1600px] p-3 pb-24 sm:p-6 sm:pb-24 lg:p-8"><Outlet /></main>
      </div>

      <nav className={cn("fixed inset-x-0 bottom-0 z-40 border-t border-[#eee7ea] bg-white/95 px-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-2 shadow-[0_-6px_18px_rgb(41_37_42_/_0.08)] backdrop-blur lg:hidden", staffOnly ? "grid grid-cols-2" : "grid grid-cols-4")} aria-label="إجراءات سريعة">
        {staffOnly ? <><MobileQuickAction to="/orders/new" icon={CirclePlus} label="أوردر جديد" /><MobileQuickAction to="/orders/today" icon={ReceiptText} label="أوردرات النهارده" /></> : <><MobileQuickAction to="/dashboard" icon={ReceiptText} label="الرئيسية" /><MobileQuickAction to="/orders/new" icon={CirclePlus} label="أوردر جديد" /><MobileQuickAction to="/expenses?new=1" icon={WalletCards} label="مصروف جديد" /><button type="button" onClick={() => setSidebarOpen(true)} className="mx-1 flex min-h-13 flex-col items-center justify-center gap-1 rounded-2xl px-2 text-center text-[11px] font-black text-[#736873] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A83868]" aria-label="فتح القائمة والمتقدم"><Menu className="h-5 w-5" aria-hidden="true" /><span>المزيد</span></button></>}
      </nav>
    </div>
  );
}

function MobileQuickAction({ to, icon: Icon, label }: { to: string; icon: typeof CirclePlus; label: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) => cn("mx-1 flex min-h-13 flex-col items-center justify-center gap-1 rounded-2xl px-2 text-center text-[11px] font-black text-[#736873] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A83868]", isActive && "bg-[#FCEAF2] text-[#A83868]")}
    >
      <Icon className="h-5 w-5" aria-hidden="true" />
      <span className="truncate">{label}</span>
    </NavLink>
  );
}
