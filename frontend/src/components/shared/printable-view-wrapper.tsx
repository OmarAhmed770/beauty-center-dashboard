import { Printer } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { useBusinessProfile } from "@/features/business/currency-context";
import { cn } from "@/lib/utils";

export function PrintableViewWrapper({ children, title, className, allowReceipt = false }: { children: ReactNode; title?: string; className?: string; allowReceipt?: boolean }) {
  const { profile } = useBusinessProfile();
  const [layout, setLayout] = useState<"a4" | "receipt">("a4");
  const print = (nextLayout: "a4" | "receipt") => {
    setLayout(nextLayout);
    window.setTimeout(() => window.print(), 0);
  };
  return (
    <section className={cn("printable-view rounded-[1.5rem] bg-white p-4 shadow-[0_10px_30px_rgb(41_37_42_/_0.04)] sm:p-6", layout === "receipt" ? "print-layout-receipt" : "print-layout-a4", className)}>
      <div className="mb-5 flex flex-col gap-3 border-b border-[#eee7ea] pb-4 sm:flex-row sm:items-center sm:justify-between print:hidden">
        <p className="font-black text-stone-900">{title ?? "معاينة الطباعة"}</p>
        <div className="grid grid-cols-1 gap-2 min-[390px]:grid-cols-2 sm:flex">
          <Button className="w-full sm:w-auto" variant="outline" onClick={() => print("a4")}><Printer className="h-4 w-4" />طباعة A4</Button>
          {allowReceipt && <Button className="w-full sm:w-auto" variant="secondary" onClick={() => print("receipt")}><Printer className="h-4 w-4" />إيصال حراري</Button>}
        </div>
      </div>
      {profile && <header className="mb-5 hidden border-b border-stone-200 pb-4 print:flex print:items-start print:justify-between print:gap-5" dir="rtl">
        <div className="min-w-0"><p className="text-lg font-black text-stone-950">{profile.businessName}</p>{profile.phone && <p className="mt-1 text-xs text-stone-600" dir="ltr">{profile.phone}</p>}{profile.address && <p className="mt-1 text-xs text-stone-600">{profile.address}</p>}</div>
        {profile.logoDataUrl && <img className="h-14 w-14 object-contain" src={profile.logoDataUrl} alt="شعار المركز" />}
      </header>}
      {children}
      {profile?.receiptInformation && <p className="mt-6 hidden border-t border-stone-200 pt-3 text-center text-xs text-stone-600 print:block">{profile.receiptInformation}</p>}
    </section>
  );
}
