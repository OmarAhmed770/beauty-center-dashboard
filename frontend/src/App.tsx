import { lazy, Suspense } from "react";
import { useQuery } from "@tanstack/react-query";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/layout/app-shell";
import { LoadingState } from "@/components/shared/loading-state";
import { useAuth } from "@/features/auth/auth-context";
import { PermissionRoute, ProtectedRoute, RoleRoute } from "@/features/auth/route-guards";
import { requestInitialSetupStatus } from "@/lib/api/auth";

const LoginPage = lazy(() => import("@/features/auth/login-page").then((module) => ({ default: module.LoginPage })));
const StaffRegistrationPage = lazy(() => import("@/features/auth/staff-registration-page").then((module) => ({ default: module.StaffRegistrationPage })));
const EmployeesPage = lazy(() => import("@/features/employees/employees-page").then((module) => ({ default: module.EmployeesPage })));
const EmployeeDetailsPage = lazy(() => import("@/features/employees/employee-details-page").then((module) => ({ default: module.EmployeeDetailsPage })));
const EmployeeAttendancePage = lazy(() => import("@/features/employees/attendance-page").then((module) => ({ default: module.EmployeeAttendancePage })));
const ExpensesPage = lazy(() => import("@/features/expenses/expenses-page").then((module) => ({ default: module.ExpensesPage })));
const ExpenseSettingsPage = lazy(() => import("@/features/expenses/expense-settings-page").then((module) => ({ default: module.ExpenseSettingsPage })));
const CatalogPage = lazy(() => import("@/features/catalog/catalog-page").then((module) => ({ default: module.CatalogPage })));
const CatalogCategoriesPage = lazy(() => import("@/features/catalog/catalog-categories-page").then((module) => ({ default: module.CatalogCategoriesPage })));
const PriceHistoryPage = lazy(() => import("@/features/catalog/price-history-page").then((module) => ({ default: module.PriceHistoryPage })));
const DiscountsPage = lazy(() => import("@/features/discounts/discounts-page").then((module) => ({ default: module.DiscountsPage })));
const HairPage = lazy(() => import("@/features/hair/hair-page").then((module) => ({ default: module.HairPage })));
const SimpleOrderPage = lazy(() => import("@/features/sales/simple-order-page").then((module) => ({ default: module.SimpleOrderPage })));
const TodayOrdersPage = lazy(() => import("@/features/sales/today-orders-page").then((module) => ({ default: module.TodayOrdersPage })));
const SalesPage = lazy(() => import("@/features/sales/sales-page").then((module) => ({ default: module.SalesPage })));
const InvoiceBuilderPage = lazy(() => import("@/features/sales/invoice-builder-page").then((module) => ({ default: module.InvoiceBuilderPage })));
const InvoiceDetailsPage = lazy(() => import("@/features/sales/invoice-details-page").then((module) => ({ default: module.InvoiceDetailsPage })));
const DashboardPage = lazy(() => import("@/pages/dashboard-page").then((module) => ({ default: module.DashboardPage })));
const ForbiddenPage = lazy(() => import("@/pages/forbidden-page").then((module) => ({ default: module.ForbiddenPage })));
const ReportsPage = lazy(() => import("@/features/reports/reports-page").then((module) => ({ default: module.ReportsPage })));
const DailyReportPage = lazy(() => import("@/features/reports/daily-report-page").then((module) => ({ default: module.DailyReportPage })));
const FinancialReportPage = lazy(() => import("@/features/reports/financial-report-page").then((module) => ({ default: module.FinancialReportPage })));
const EmployeeReportPage = lazy(() => import("@/features/reports/employee-report-page").then((module) => ({ default: module.EmployeeReportPage })));
const ExpenseReportPage = lazy(() => import("@/features/reports/expense-report-page").then((module) => ({ default: module.ExpenseReportPage })));
const SalesReportPage = lazy(() => import("@/features/reports/sales-report-page").then((module) => ({ default: module.SalesReportPage })));
const PaymentReportPage = lazy(() => import("@/features/reports/payment-report-page").then((module) => ({ default: module.PaymentReportPage })));
const ProfitReportPage = lazy(() => import("@/features/reports/profit-report-page").then((module) => ({ default: module.ProfitReportPage })));
const PriceHistoryReportPage = lazy(() => import("@/features/reports/price-history-report-page").then((module) => ({ default: module.PriceHistoryReportPage })));
const DailyClosingPage = lazy(() => import("@/features/closing/daily-closing-page").then((module) => ({ default: module.DailyClosingPage })));
const SettingsPage = lazy(() => import("@/features/backup/settings-page").then((module) => ({ default: module.SettingsPage })));
const SetupPage = lazy(() => import("@/features/setup/setup-page").then((module) => ({ default: module.SetupPage })));
const UsersPage = lazy(() => import("@/features/users/users-page").then((module) => ({ default: module.UsersPage })));

function HomeRedirect() {
  const { hasPermission } = useAuth();
  return <Navigate to={hasPermission("DASHBOARD") ? "/dashboard" : "/orders/new"} replace />;
}

function InitialRoute() {
  const setup = useQuery({ queryKey: ["setup", "status"], queryFn: requestInitialSetupStatus, retry: false });
  if (setup.isPending) return <LoadingState fullPage label="جارٍ التحقق من إعداد النظام..." />;
  return <Navigate to={setup.data?.initialOwnerSetupRequired ? "/setup" : "/login"} replace />;
}

export default function App() {
  return (
    <Suspense fallback={<LoadingState fullPage label="جارٍ تحميل الوحدة..." />}>
      <Routes>
        <Route path="/" element={<InitialRoute />} />
        <Route path="/setup" element={<SetupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<StaffRegistrationPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="app" element={<HomeRedirect />} />
            <Route path="forbidden" element={<ForbiddenPage />} />
            <Route element={<PermissionRoute permission="ADD_ORDERS" />}>
              <Route path="orders/new" element={<SimpleOrderPage />} />
              <Route path="orders/today" element={<TodayOrdersPage />} />
              <Route path="orders/today/:invoiceId" element={<InvoiceDetailsPage backPath="/orders/today" backLabel="أوردرات النهارده" />} />
            </Route>

            <Route element={<PermissionRoute permission="EMPLOYEES" />}>
              <Route path="employees" element={<EmployeesPage />} />
              <Route path="employees/:employeeId" element={<EmployeeDetailsPage />} />
            </Route>
            <Route element={<PermissionRoute permission="EXPENSES" />}>
              <Route path="expenses" element={<ExpensesPage />} />
            </Route>
            <Route element={<PermissionRoute permission="SERVICES_AND_PRICES" />}>
              <Route path="catalog" element={<CatalogPage />} />
              <Route path="catalog/hair" element={<HairPage />} />
              <Route path="catalog/categories" element={<CatalogCategoriesPage />} />
              <Route path="catalog/items/:itemId/history" element={<PriceHistoryPage />} />
            </Route>
            <Route element={<PermissionRoute permission="SETTINGS" />}>
              <Route path="discounts" element={<DiscountsPage />} />
              <Route path="expenses/settings" element={<ExpenseSettingsPage />} />
            </Route>
            <Route element={<PermissionRoute anyOf={["SETTINGS", "BACKUP_MANAGEMENT"]} />}>
              <Route path="settings" element={<SettingsPage />} />
            </Route>
            <Route element={<PermissionRoute permission="INVOICES_AND_RETURNS" />}>
              <Route path="sales" element={<SalesPage />} />
              <Route path="sales/invoices/:invoiceId/edit" element={<InvoiceBuilderPage />} />
              <Route path="sales/invoices/:invoiceId" element={<InvoiceDetailsPage />} />
            </Route>
            <Route element={<PermissionRoute permission="DASHBOARD" />}>
              <Route path="dashboard" element={<DashboardPage />} />
            </Route>
            <Route element={<PermissionRoute permission="REPORTS" />}>
              <Route path="reports" element={<ReportsPage />} />
              <Route path="reports/daily" element={<DailyReportPage />} />
              <Route path="reports/financial" element={<FinancialReportPage />} />
              <Route path="reports/employees" element={<EmployeeReportPage />} />
              <Route path="reports/expenses" element={<ExpenseReportPage />} />
              <Route path="reports/sales" element={<SalesReportPage />} />
              <Route path="reports/payments" element={<PaymentReportPage />} />
              <Route path="reports/profit" element={<ProfitReportPage />} />
              <Route path="reports/price-history" element={<PriceHistoryReportPage />} />
            </Route>
            <Route element={<PermissionRoute permission="DAILY_CLOSING" />}>
              <Route path="daily-closing" element={<DailyClosingPage />} />
            </Route>
            <Route element={<RoleRoute allowedRoles={["OWNER"]} />}>
              <Route path="employees/attendance" element={<EmployeeAttendancePage />} />
              <Route path="users" element={<UsersPage />} />
            </Route>
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
