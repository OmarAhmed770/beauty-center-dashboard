import {
  ArrowLeft,
  BadgePercent,
  CalendarCheck,
  FileChartColumn,
  ReceiptText,
  Scissors,
  Settings,
  UsersRound,
  WalletCards,
  type LucideIcon,
} from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Card } from "@/components/ui/card";
import { useLocale } from "@/features/locale/locale-context";

interface PageCopy {
  ar: { title: string; description: string; points: string[] };
  en: { title: string; description: string; points: string[] };
  icon: LucideIcon;
}

const pages: Record<string, PageCopy> = {
  sales: {
    icon: ReceiptText,
    ar: { title: "المبيعات", description: "مساحة الفواتير ونقاط التحصيل المهيأة لعمل الاستقبال.", points: ["إنشاء فاتورة جديدة", "الدفع بأكثر من طريقة", "البحث في الفواتير"] },
    en: { title: "Sales", description: "Reception workspace for invoices and payments.", points: ["Create invoices", "Split payments", "Invoice search"] },
  },
  employees: {
    icon: UsersRound,
    ar: { title: "الموظفون", description: "إدارة بيانات الموظفين وحركاتهم المالية ستُستكمل في المرحلة التالية.", points: ["بيانات الموظفين", "الحركات المالية", "كشف حساب الموظف"] },
    en: { title: "Employees", description: "Employee profiles and transactions will be completed next.", points: ["Employee profiles", "Transactions", "Statements"] },
  },
  expenses: {
    icon: WalletCards,
    ar: { title: "المصروفات", description: "تسجيل المصروفات ومراجعة تصنيفاتها وطرق دفعها.", points: ["تسجيل المصروف", "التصفية حسب الفترة", "ملخصات التصنيفات"] },
    en: { title: "Expenses", description: "Record and review categorized expenses.", points: ["Expense entry", "Date filters", "Category summaries"] },
  },
  catalog: {
    icon: Scissors,
    ar: { title: "الخدمات والمنتجات", description: "إدارة الكتالوج والأسعار وإعدادات تركيب الشعر.", points: ["الخدمات والمنتجات", "سجل الأسعار", "إعدادات تركيب الشعر"] },
    en: { title: "Catalog", description: "Catalog, pricing, and hair configuration management.", points: ["Services and products", "Price history", "Hair configuration"] },
  },
  discounts: {
    icon: BadgePercent,
    ar: { title: "الخصومات", description: "إدارة الخصومات المجدولة وربطها بالخدمات والمنتجات.", points: ["خصومات النسبة", "خصومات القيمة", "فترات الصلاحية"] },
    en: { title: "Discounts", description: "Manage scheduled catalog discounts.", points: ["Percentage discounts", "Fixed discounts", "Validity periods"] },
  },
  reports: {
    icon: FileChartColumn,
    ar: { title: "التقارير", description: "تقارير مالية وتشغيلية جاهزة للمراجعة والطباعة.", points: ["تقرير المبيعات", "تقرير المصروفات", "تقرير الأرباح"] },
    en: { title: "Reports", description: "Financial and operational reports ready for review.", points: ["Sales report", "Expense report", "Profit report"] },
  },
  closing: {
    icon: CalendarCheck,
    ar: { title: "الإقفال اليومي", description: "مراجعة ملخص اليوم ومطابقة الرصيد النقدي قبل الإقفال.", points: ["معاينة الإقفال", "مطابقة النقدية", "طباعة الملخص"] },
    en: { title: "Daily Closing", description: "Review the day and reconcile cash before closing.", points: ["Closing preview", "Cash reconciliation", "Printable summary"] },
  },
  settings: {
    icon: Settings,
    ar: { title: "الإعدادات", description: "إعدادات النظام والبيانات المرجعية والصلاحيات.", points: ["طرق الدفع", "التصنيفات", "المستخدمون والصلاحيات"] },
    en: { title: "Settings", description: "System, reference data, and access settings.", points: ["Payment methods", "Categories", "Users and access"] },
  },
};

export function PlaceholderPage({ page }: { page: keyof typeof pages }) {
  const { locale } = useLocale();
  const configuration = pages[page];
  const copy = configuration[locale];
  const Icon = configuration.icon;

  return (
    <div className="space-y-7">
      <PageHeader title={copy.title} description={copy.description} />
      <Card className="overflow-hidden">
        <div className="grid min-h-[360px] lg:grid-cols-[1fr_0.75fr]">
          <div className="flex flex-col justify-center p-7 sm:p-10">
            <span className="grid h-14 w-14 place-items-center rounded-2xl bg-rose-100 text-rose-800"><Icon className="h-7 w-7" aria-hidden="true" /></span>
            <h2 className="mt-6 text-xl font-black text-stone-950">الواجهة جاهزة للربط</h2>
            <p className="mt-3 max-w-xl text-sm leading-7 text-stone-600">تم تجهيز المسار والصلاحيات والتخطيط المشترك. تفاصيل هذه الوحدة ستُبنى في مرحلتها المخصصة دون وضع منطق أعمال مؤقت هنا.</p>
          </div>
          <div className="border-t border-stone-200 bg-stone-50 p-7 sm:p-10 lg:border-s lg:border-t-0">
            <p className="text-xs font-black text-stone-500">نطاق الوحدة</p>
            <ul className="mt-5 space-y-3">
              {copy.points.map((point) => <li key={point} className="flex items-center justify-between rounded-xl border border-stone-200 bg-white px-4 py-3 text-sm font-bold text-stone-700"><span>{point}</span><ArrowLeft className="h-4 w-4 text-stone-400" /></li>)}
            </ul>
          </div>
        </div>
      </Card>
    </div>
  );
}
