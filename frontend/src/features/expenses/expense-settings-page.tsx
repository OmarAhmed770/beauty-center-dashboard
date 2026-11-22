import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { CategoryManagement } from "@/features/expense-categories/category-management";
import { PaymentMethodManagement } from "@/features/payment-methods/payment-method-management";

export function ExpenseSettingsPage() {
  return (
    <div className="space-y-8">
      <PageHeader title="إعدادات المصروفات" description="إدارة تصنيفات المصروفات وطرق الدفع القابلة للاستخدام." actions={<Button asChild variant="outline"><Link to="/expenses"><ArrowRight className="h-4 w-4" />قائمة المصروفات</Link></Button>} />
      <CategoryManagement />
      <PaymentMethodManagement />
    </div>
  );
}
