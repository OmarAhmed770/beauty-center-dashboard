import {
  CalendarCheck,
  FileChartColumn,
  LayoutDashboard,
  PlusCircle,
  ReceiptText,
  Scissors,
  Settings,
  UsersRound,
  WalletCards,
  type LucideIcon,
} from "lucide-react";
import type { TranslationKey } from "@/features/locale/locale-context";
import type { SystemPermission } from "@/types/auth";

export interface NavigationItem {
  path: string;
  labelKey: TranslationKey;
  icon: LucideIcon;
  permission?: SystemPermission;
  anyPermissions?: readonly SystemPermission[];
}

export const primaryNavigationItems: NavigationItem[] = [
  { path: "/dashboard", labelKey: "dashboard", icon: LayoutDashboard, permission: "DASHBOARD" },
  { path: "/orders/new", labelKey: "addOrder", icon: PlusCircle, permission: "ADD_ORDERS" },
  { path: "/orders/today", labelKey: "todayOrders", icon: ReceiptText, permission: "ADD_ORDERS" },
  { path: "/expenses", labelKey: "expenses", icon: WalletCards, permission: "EXPENSES" },
  { path: "/catalog", labelKey: "servicesPrices", icon: Scissors, permission: "SERVICES_AND_PRICES" },
  { path: "/employees", labelKey: "employees", icon: UsersRound, permission: "EMPLOYEES" },
  { path: "/daily-closing", labelKey: "closing", icon: CalendarCheck, permission: "DAILY_CLOSING" },
];

export const advancedNavigationItems: NavigationItem[] = [
  { path: "/reports", labelKey: "reports", icon: FileChartColumn, permission: "REPORTS" },
  { path: "/sales", labelKey: "invoicesRefunds", icon: ReceiptText, permission: "INVOICES_AND_RETURNS" },
  { path: "/settings", labelKey: "settings", icon: Settings, anyPermissions: ["SETTINGS", "BACKUP_MANAGEMENT"] },
];

/** Compatibility export for existing callers and tests. */
export const navigationItems = [...primaryNavigationItems, ...advancedNavigationItems];

export function visiblePrimaryNavigation(permissions: readonly SystemPermission[] | undefined, owner = false): NavigationItem[] {
  return primaryNavigationItems.filter((item) => item.path !== "/orders/today" || !owner)
    .filter((item) => canAccessNavigationItem(item, permissions, owner));
}

export function visibleAdvancedNavigation(permissions: readonly SystemPermission[] | undefined, owner = false): NavigationItem[] {
  return advancedNavigationItems.filter((item) => canAccessNavigationItem(item, permissions, owner));
}

function canAccessNavigationItem(
  item: NavigationItem,
  permissions: readonly SystemPermission[] | undefined,
  owner: boolean,
): boolean {
  if (owner) return true;
  if (item.permission) return Boolean(permissions?.includes(item.permission));
  return Boolean(item.anyPermissions?.some((permission) => permissions?.includes(permission)));
}
