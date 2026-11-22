import { ShieldX } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export function ForbiddenPage() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="max-w-lg text-center">
        <span className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-red-100 text-red-700"><ShieldX className="h-8 w-8" /></span>
        <h1 className="mt-6 text-2xl font-black text-stone-950">ليس لديك صلاحية لهذه الصفحة</h1>
        <p className="mt-3 text-sm leading-7 text-stone-600">يمكنك الرجوع إلى مساحة العمل المتاحة لحسابك. يتحقق الخادم أيضًا من كل صلاحية عند تنفيذ الطلبات.</p>
        <Button asChild className="mt-6"><Link to="/">العودة إلى الصفحة الرئيسية</Link></Button>
      </div>
    </div>
  );
}
