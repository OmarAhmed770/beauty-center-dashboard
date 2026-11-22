import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("rounded-[1.25rem] border border-soft-border bg-white/95 shadow-[0_12px_30px_rgb(58_37_48_/_0.06)]", className)} {...props} />;
}
