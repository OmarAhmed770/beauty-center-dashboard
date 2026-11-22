import { forwardRef, type ChangeEvent, type InputHTMLAttributes } from "react";
import { Input } from "@/components/ui/input";
import { getActiveCurrencySymbol } from "@/lib/format";
import { cn } from "@/lib/utils";

function normalizeDigits(value: string): string {
  return value
    .replace(/[٠-٩]/g, (digit) => String("٠١٢٣٤٥٦٧٨٩".indexOf(digit)))
    .replace(/[۰-۹]/g, (digit) => String("۰۱۲۳۴۵۶۷۸۹".indexOf(digit)))
    .replace(/٫/g, ".");
}

type MoneyInputProps = InputHTMLAttributes<HTMLInputElement> & { suffix?: string };

export const MoneyInput = forwardRef<HTMLInputElement, MoneyInputProps>(
  ({ className, onChange, suffix = getActiveCurrencySymbol(), ...props }, ref) => {
    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
      event.currentTarget.value = normalizeDigits(event.currentTarget.value).replace(/[^0-9.]/g, "");
      onChange?.(event);
    };

    return (
      <div className="relative">
        <Input
          ref={ref}
          type="text"
          inputMode="decimal"
          dir="ltr"
          className={cn("pr-3 pl-14 text-left tabular-nums", className)}
          onChange={handleChange}
          {...props}
        />
        <span className="pointer-events-none absolute left-3 top-3.5 text-sm font-bold text-stone-500">{suffix}</span>
      </div>
    );
  },
);

MoneyInput.displayName = "MoneyInput";
