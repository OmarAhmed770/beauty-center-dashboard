import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

type StatusTone = "success" | "warning" | "danger" | "info" | "neutral";

const tones: Record<StatusTone, string> = {
  success: "bg-success/10 text-success ring-success/20",
  warning: "bg-[#fff1eb] text-[#a65335] ring-[#f3d0c0]",
  danger: "bg-danger/10 text-danger ring-danger/20",
  info: "bg-[#edf4fb] text-[#3f6896] ring-[#cbdced]",
  neutral: "bg-page text-muted ring-soft-border",
};

export function StatusBadge({ children, tone = "neutral", className }: { children: ReactNode; tone?: StatusTone; className?: string }) {
  return <span className={cn("inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset", tones[tone], className)}>{children}</span>;
}
