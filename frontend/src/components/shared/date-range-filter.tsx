import { CalendarDays, ChevronDown } from "lucide-react";
import { Input } from "@/components/ui/input";
import { useLocale } from "@/features/locale/locale-context";

export type DatePreset = "TODAY" | "YESTERDAY" | "CURRENT_WEEK" | "CURRENT_MONTH" | "CUSTOM";

export interface DateRangeValue {
  preset: DatePreset;
  from: string;
  to: string;
}

const presetLabels = {
  ar: {
    TODAY: "اليوم",
    YESTERDAY: "أمس",
    CURRENT_WEEK: "الأسبوع الحالي",
    CURRENT_MONTH: "الشهر الحالي",
    CUSTOM: "فترة مخصصة",
  },
  en: {
    TODAY: "Today",
    YESTERDAY: "Yesterday",
    CURRENT_WEEK: "Current week",
    CURRENT_MONTH: "Current month",
    CUSTOM: "Custom range",
  },
} as const;

export function cairoDateIso(date = new Date()): string {
  const values = Object.fromEntries(
    new Intl.DateTimeFormat("en-US", {
      timeZone: "Africa/Cairo",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    })
      .formatToParts(date)
      .map(({ type, value }) => [type, value]),
  );
  return `${values.year}-${values.month}-${values.day}`;
}

function shiftIso(iso: string, amount: number): string {
  const date = new Date(`${iso}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() + amount);
  return date.toISOString().slice(0, 10);
}

export function rangeForPreset(preset: DatePreset): DateRangeValue {
  const today = cairoDateIso();
  if (preset === "YESTERDAY") {
    const yesterday = shiftIso(today, -1);
    return { preset, from: yesterday, to: yesterday };
  }
  if (preset === "CURRENT_WEEK") {
    const weekday = new Date(`${today}T12:00:00Z`).getUTCDay();
    const mondayOffset = weekday === 0 ? -6 : 1 - weekday;
    return { preset, from: shiftIso(today, mondayOffset), to: today };
  }
  if (preset === "CURRENT_MONTH") {
    return { preset, from: `${today.slice(0, 7)}-01`, to: today };
  }
  return { preset, from: today, to: today };
}

export function DateRangeFilter({ value, onChange }: { value: DateRangeValue; onChange: (value: DateRangeValue) => void }) {
  const { locale } = useLocale();
  const presets = Object.keys(presetLabels.ar) as DatePreset[];

  return (
    <>
      <details className="group rounded-[1.5rem] border border-[#eee7ea] bg-white shadow-[0_10px_30px_rgb(41_37_42_/_0.04)] sm:hidden">
        <summary className="flex min-h-12 cursor-pointer list-none items-center justify-between gap-3 px-4 py-3 text-sm font-black text-[#29252A] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-[#A83868]">
          <span className="flex min-w-0 items-center gap-2"><CalendarDays className="h-5 w-5 shrink-0 text-[#A83868]" aria-hidden="true" />{presetLabels[locale][value.preset]}</span>
          <ChevronDown className="h-5 w-5 shrink-0 text-[#736873] transition-transform group-open:rotate-180" aria-hidden="true" />
        </summary>
        <div className="border-t border-[#f1ebee] p-4">
          <PresetButtons presets={presets} value={value} onChange={onChange} locale={locale} />
          <CustomRangeInputs value={value} onChange={onChange} />
        </div>
      </details>
      <fieldset className="hidden rounded-[1.5rem] border border-[#eee7ea] bg-white p-4 shadow-[0_10px_30px_rgb(41_37_42_/_0.04)] sm:block">
        <legend className="sr-only">اختيار الفترة الزمنية</legend>
        <div className="flex flex-wrap items-center gap-2">
          <CalendarDays className="ms-1 h-5 w-5 text-[#A83868]" aria-hidden="true" />
          <PresetButtons presets={presets} value={value} onChange={onChange} locale={locale} />
        </div>
        <CustomRangeInputs value={value} onChange={onChange} />
      </fieldset>
    </>
  );
}

function PresetButtons({ presets, value, onChange, locale }: { presets: DatePreset[]; value: DateRangeValue; onChange: (value: DateRangeValue) => void; locale: "ar" | "en" }) {
  return <>{presets.map((preset) => <button key={preset} type="button" className={value.preset === preset ? "min-h-11 rounded-2xl bg-[#A83868] px-4 py-2 text-sm font-bold text-white" : "min-h-11 rounded-2xl bg-[#F9F9F9] px-4 py-2 text-sm font-bold text-[#736873] hover:bg-[#FCEAF2] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#A83868]"} onClick={() => onChange(rangeForPreset(preset))}>{presetLabels[locale][preset]}</button>)}</>;
}

function CustomRangeInputs({ value, onChange }: { value: DateRangeValue; onChange: (value: DateRangeValue) => void }) {
  if (value.preset !== "CUSTOM") return null;
  return <div className="mt-4 grid gap-4 border-t border-[#f1ebee] pt-4 sm:grid-cols-2"><label className="text-sm font-bold text-[#4b454b]">من تاريخ<Input className="mt-2" type="date" value={value.from} onChange={(event) => onChange({ ...value, from: event.target.value })} /></label><label className="text-sm font-bold text-[#4b454b]">إلى تاريخ<Input className="mt-2" type="date" value={value.to} onChange={(event) => onChange({ ...value, to: event.target.value })} /></label></div>;
}
